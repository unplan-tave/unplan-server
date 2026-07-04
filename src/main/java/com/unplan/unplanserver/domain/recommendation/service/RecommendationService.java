package com.unplan.unplanserver.domain.recommendation.service;

import com.unplan.unplanserver.domain.measurement.service.MeasurementService;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationAcceptResponse;
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
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
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
    /** 한 번에 노출하는 최대 추천 수 (PM 확정 2026-07-04: Figma는 4개지만 부담을 줄이려 3개로 축소) */
    static final int MAX_RECOMMENDATIONS = 3;

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
            // 우선순위 티어를 이어 채워 최대 MAX_RECOMMENDATIONS 개 (PM 확정 2026-07-04)
            List<QueueCard> top = matcher.match(current, fitting, MAX_RECOMMENDATIONS, slot.durationMinutes());
            // TODO(task B — 회복 수단, PM 확정 2026-07-04): '기력 회복'이면 기력회복 카드 뒤에 '회복 수단' 후보 1건을
            //  덧붙여 총 MAX_RECOMMENDATIONS 를 채운다. 회복 수단은 개별 수단(낮잠/음악…)이 아니라 하나의 후보이고
            //  수락 시 사용자가 고른 수단이 일정 제목이 된다. 온보딩 Recover 순서대로 옵션 제공.
            //  길이는 min(빈 시간, 30분), sourceType=RECOVERY_MEAN, sourceScheduleId=null.
            //  온보딩이 회복방법 ≥1 을 강제하므로(RECOVER_METHOD_REQUIRED) 기력 회복 상태에선 이 후보가 항상 ≥1개 →
            //  0개 오류 케이스는 ①빈시간X ②빈시간O·매칭카드X 둘뿐(기력회복 0개 케이스는 발생하지 않음).
            if (top.isEmpty()) continue;

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
     * 추천 수락. (2026-07-03 결정, 이슈 #78)
     * <ul>
     *   <li>큐 카드 추천: 기본은 원본 큐 카드에 날짜·시간을 부여해 핀 카드로 '전환'(원본 UPDATE, 큐에서 사라짐).
     *       {@code keepQueueCard=true}('기존 큐 카드 유지하기')면 핀 카드를 복제 생성하고 원본 큐 카드는 그대로 두어,
     *       같은 일정이 큐(다음 추천 후보로 계속 노출)와 핀(확정)으로 공존한다.</li>
     *   <li>회복 수단 추천: 원본 큐 카드가 없으므로 항상 새 일정 생성(INSERT).</li>
     * </ul>
     */
    @Transactional
    public RecommendationAcceptResponse accept(Long memberId, Long recommendId, boolean keepQueueCard) {
        Recommendation rec = recommendationRepository.findByRecommendIdAndMemberId(recommendId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        if (rec.getStatus() != RecommendationStatus.PENDING) {
            throw new CustomException(ErrorCode.RECOMMENDATION_ALREADY_PROCESSED);
        }

        // 스케줄 모델은 자정을 넘기는 구간을 표현할 수 없다(start<end). 슬롯 끝이 정확히 24:00이면 추천
        // 스냅샷 end 는 00:00(=24:00 규약)으로 저장되므로, 핀 카드로는 23:59 로 클램핑해 유효 구간을 유지한다.
        LocalTime endTime = LocalTime.MIDNIGHT.equals(rec.getEndTime()) ? LocalTime.of(23, 59) : rec.getEndTime();

        Long scheduleId;
        boolean created;
        if (rec.getSourceType() == RecommendationSourceType.QUEUE_CARD) {
            // 원본 큐 카드가 이미 삭제됐으면 전환/복제 대상이 없다.
            Schedule source = scheduleRepository.findByScheduleIdAndMemberId(rec.getSourceScheduleId(), memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
            if (keepQueueCard) {
                // 큐 카드는 그대로 두고 핀 카드를 복제 생성 → 같은 일정이 큐+핀으로 공존
                Schedule pin = scheduleRepository.save(
                        pinCopyOf(memberId, source, rec.getDate(), rec.getStartTime(), endTime));
                scheduleId = pin.getScheduleId();
                created = true;
            } else {
                // 큐 카드 → 핀 카드 전환 (원본 UPDATE)
                source.assignToPin(rec.getDate(), rec.getStartTime(), endTime);
                scheduleId = source.getScheduleId();
                created = false;
            }
        } else {
            // 회복 수단 → 원본 큐 카드가 없으므로 새 일정 INSERT
            Schedule saved = scheduleRepository.save(Schedule.builder()
                    .memberId(memberId)
                    .title(rec.getTitle())
                    .conditionTag(rec.getConditionTag())
                    .date(rec.getDate())
                    .startTime(rec.getStartTime())
                    .endTime(endTime)
                    .isQueue(false)
                    .isRecurring(false)
                    .isConflict(false)
                    .status(ScheduleStatus.TODO)
                    .build());
            scheduleId = saved.getScheduleId();
            created = true;
        }

        rec.accept(scheduleId);
        return new RecommendationAcceptResponse(
                rec.getRecommendId(), scheduleId, rec.getTitle(),
                rec.getDate(), rec.getStartTime(), endTime,
                rec.getSourceType().name(), created);
    }

    /**
     * 큐 카드를 핀 카드로 복제한다. 제목·컨디션 태그·위치·메모·알림 설정은 원본 그대로 유지하고
     * 날짜·시간만 부여해 핀 카드로 만든다(Figma "제목/태그/위치/메모는 원래 큐 카드대로 유지").
     * 반복 설정은 복제하지 않는다(단발성 핀 카드).
     */
    private Schedule pinCopyOf(Long memberId, Schedule source, LocalDate date, LocalTime start, LocalTime end) {
        return Schedule.builder()
                .memberId(memberId)
                .title(source.getTitle())
                .location(source.getLocation())
                .conditionTag(source.getConditionTag())
                .estimatedTime(source.getEstimatedTime())
                .memo(source.getMemo())
                .isRemindOn(source.getIsRemindOn())
                .remindMinutes(source.getRemindMinutes())
                .remindType(source.getRemindType())
                .remindSoundType(source.getRemindSoundType())
                .date(date)
                .startTime(start)
                .endTime(end)
                .isQueue(false)
                .isRecurring(false)
                .isConflict(false)
                .status(ScheduleStatus.TODO)
                .build();
    }

    /**
     * 추천 거절. REJECTED 로 표시해 같은 날짜 재계산 시 목록에서 제외한다("다시 안 뜸").
     * 큐 카드 원본은 건드리지 않는다.
     */
    @Transactional
    public void reject(Long memberId, Long recommendId) {
        Recommendation rec = recommendationRepository.findByRecommendIdAndMemberId(recommendId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        if (rec.getStatus() != RecommendationStatus.PENDING) {
            throw new CustomException(ErrorCode.RECOMMENDATION_ALREADY_PROCESSED);
        }
        rec.reject();
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
