package com.unplan.unplanserver.domain.recommendation.service;

import com.unplan.unplanserver.domain.measurement.service.MeasurementService;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse.EmptyTime;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse.RecommendationItem;
import com.unplan.unplanserver.domain.recommendation.engine.EmptyTimeFinder;
import com.unplan.unplanserver.domain.recommendation.engine.QueueCard;
import com.unplan.unplanserver.domain.recommendation.engine.RecommendationMatcher;
import com.unplan.unplanserver.domain.recommendation.engine.TimeRange;
import com.unplan.unplanserver.domain.recommendation.engine.TimeSlot;
import com.unplan.unplanserver.domain.recommendation.entity.Recommendation;
import com.unplan.unplanserver.domain.recommendation.enums.RecommendationSourceType;
import com.unplan.unplanserver.domain.recommendation.enums.RecommendationStatus;
import com.unplan.unplanserver.domain.recommendation.repository.RecommendationRepository;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 홈/컨디션 탭 일정 추천 생성 (Notion "추천 로직" 1~4단계 조립).
 * 빈 시간 탐색 → 소요시간 필터 → 컨디션 태그 매칭 → 정렬 → 슬롯 배치 → 영속화.
 *
 * GET 시마다 이전 노출분(PENDING)을 지우고 재생성한다 — 큐 카드/핀 카드/컨디션이 수시로 바뀌므로.
 * 수락(ACCEPTED)/거절(REJECTED) 이력은 남기며, 거절된 원본 큐 카드는 같은 날짜 재계산에서 제외한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    /** 핀 카드 앞뒤 버퍼(분) — Notion 1-4 */
    static final int BUFFER_MINUTES = 15;
    /** '최소 여유 시간' 기본값. #83(빈시간 추천 설정) 머지 후 회원 설정값 read 로 교체 */
    static final int DEFAULT_MIN_GAP_MINUTES = 0;
    /** 한 번에 노출하는 최대 추천 수 — Figma 바텀시트 "1/4" 페이지네이션 */
    static final int MAX_RECOMMENDATIONS = 4;

    private final ScheduleService scheduleService;
    private final ScheduleRepository scheduleRepository;
    private final RecommendationRepository recommendationRepository;
    private final MeasurementService measurementService;
    private final EmptyTimeFinder emptyTimeFinder;
    private final RecommendationMatcher matcher;

    @Transactional
    public RecommendationListResponse getRecommendations(Long memberId, LocalDate date) {
        return generate(memberId, date, LocalDateTime.now());
    }

    /** now 를 주입받는 내부 진입점 (테스트 용이성) */
    @Transactional
    public RecommendationListResponse generate(Long memberId, LocalDate date, LocalDateTime now) {
        // 과거 날짜는 추천할 빈 시간이 없다 → 빈 응답 (탐색 범위: 현재 시각 이후 ~ 그날 자정)
        if (date.isBefore(now.toLocalDate())) {
            return new RecommendationListResponse(date, null, null, List.of());
        }

        // 1. 현재 컨디션 태그 — Notion: "현재 컨디션을 오늘의 나머지 시간에 동일하게 적용"
        ConditionTag current = currentConditionTag(memberId, now.toLocalDate());

        // 이전 노출분 정리 후 재생성 (수락/거절 이력은 보존)
        recommendationRepository.deleteByMemberIdAndDateAndStatus(memberId, date, RecommendationStatus.PENDING);

        // 2. 빈 시간 탐색 — 반복 인스턴스를 포함한 핀 카드가 busy
        LocalTime windowStart = date.equals(now.toLocalDate()) ? now.toLocalTime() : LocalTime.MIDNIGHT;
        List<TimeRange> busy = scheduleService.findSchedulesWithRecurring(memberId, date).stream()
                .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                .map(s -> new TimeRange(s.getStartTime(), s.getEndTime()))
                .toList();
        List<TimeSlot> slots = emptyTimeFinder.findFreeSlots(
                date, windowStart, LocalTime.MIDNIGHT, busy, BUFFER_MINUTES, DEFAULT_MIN_GAP_MINUTES);

        // 3. 추천 후보 큐 카드 — 완료·소요시간 미정(Notion 2-2)·이 날짜에 거절된 카드 제외
        Set<Long> rejectedSourceIds = recommendationRepository
                .findByMemberIdAndDateAndStatusIn(memberId, date, List.of(RecommendationStatus.REJECTED))
                .stream()
                .map(Recommendation::getSourceScheduleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Schedule> candidateById = scheduleRepository.findByMemberIdAndIsQueueTrue(memberId).stream()
                .filter(s -> s.getStatus() != ScheduleStatus.DONE)
                .filter(s -> s.getEstimatedTime() != null)
                .filter(s -> !rejectedSourceIds.contains(s.getScheduleId()))
                .collect(Collectors.toMap(Schedule::getScheduleId, Function.identity()));
        List<QueueCard> cards = candidateById.values().stream()
                .map(s -> new QueueCard(s.getScheduleId(), s.getConditionTag(),
                        s.getEstimatedTime(), s.getDate(), s.getCreatedAt()))
                .toList();

        // 4. 슬롯을 시간순으로 돌며, 처음으로 후보가 나오는 빈 시간에 추천 배치
        //    (Figma: 바텀시트는 하나의 빈 시간 + 그 안의 추천 카드 페이지네이션)
        for (TimeSlot slot : slots) {
            List<QueueCard> fitting = cards.stream()
                    .filter(c -> c.estimatedMinutes() <= slot.durationMinutes())
                    .toList();
            List<QueueCard> matched = matcher.matchByTag(current, fitting);
            // TODO(기획 확인 대기 — 이슈 #78): '기력 회복'일 때 온보딩 회복 수단 추천
            //  폴백(기력회복 카드 없을 때만) vs 병렬(카드 뒤에 항상) / 랜덤 vs 설정 순서 확정 후 여기에 붙인다.
            //  회복 수단 길이는 min(빈 시간, 30분), sourceType=RECOVERY_MEAN, sourceScheduleId=null.
            if (matched.isEmpty()) continue;

            List<QueueCard> top = matcher.sort(matched, slot.durationMinutes()).stream()
                    .limit(MAX_RECOMMENDATIONS)
                    .toList();
            return persistAndRespond(memberId, date, current, slot, top, candidateById);
        }

        // 어떤 빈 시간에도 넣을 후보가 없음
        return new RecommendationListResponse(date, current.name(), null, List.of());
    }

    private RecommendationListResponse persistAndRespond(Long memberId, LocalDate date, ConditionTag current,
                                                         TimeSlot slot, List<QueueCard> top,
                                                         Map<Long, Schedule> candidateById) {
        List<RecommendationItem> items = new ArrayList<>();
        for (int order = 0; order < top.size(); order++) {
            QueueCard card = top.get(order);
            Schedule source = candidateById.get(card.scheduleId());
            LocalTime startTime = slot.start();
            // 슬롯 안에 들어가는 카드만 오므로 wrap 은 정확히 24:00(=00:00 규약)에서만 발생
            LocalTime endTime = startTime.plusMinutes(card.estimatedMinutes());

            Recommendation saved = recommendationRepository.save(Recommendation.builder()
                    .memberId(memberId)
                    .date(date)
                    .title(source.getTitle())
                    .startTime(startTime)
                    .endTime(endTime)
                    .conditionTag(card.conditionTag())
                    .sourceType(RecommendationSourceType.QUEUE_CARD)
                    .sourceScheduleId(card.scheduleId())
                    .displayOrder(order)
                    .build());

            items.add(new RecommendationItem(
                    saved.getRecommendId(),
                    source.getTitle(),
                    startTime,
                    endTime,
                    card.estimatedMinutes(),
                    card.deadline(),
                    card.conditionTag() != null ? card.conditionTag().name() : null,
                    RecommendationSourceType.QUEUE_CARD.name(),
                    order
            ));
        }
        EmptyTime emptyTime = new EmptyTime(slot.start(), slot.end(), slot.durationMinutes());
        return new RecommendationListResponse(date, current.name(), emptyTime, items);
    }

    /**
     * 컨디션 점수 모듈(#79)의 한글 태그를 enum 으로 변환.
     * 알 수 없는 라벨이면 추천 전체를 죽이는 대신 중립 태그(일상 작업)로 폴백하고 경고를 남긴다.
     */
    private ConditionTag currentConditionTag(Long memberId, LocalDate today) {
        String label = measurementService.getDailyRecord(memberId, today).conditionTag();
        try {
            return ConditionTag.fromLabel(label);
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 컨디션 태그 라벨 '{}' — DAILY_TASK 로 폴백 (memberId={})", label, memberId);
            return ConditionTag.DAILY_TASK;
        }
    }
}
