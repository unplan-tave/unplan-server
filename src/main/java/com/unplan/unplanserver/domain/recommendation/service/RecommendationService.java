package com.unplan.unplanserver.domain.recommendation.service;

import com.unplan.unplanserver.domain.measurement.service.MeasurementService;
import com.unplan.unplanserver.domain.onboarding.entity.Biorhythm;
import com.unplan.unplanserver.domain.onboarding.repository.BiorhythmRepository;
import com.unplan.unplanserver.domain.onboarding.service.RecoverService;
import com.unplan.unplanserver.domain.recommendation.dto.response.QueueCardRecommendationResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.QueueCardRecommendationResult;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationAcceptResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse.EmptyTime;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse.RecommendationItem;
import com.unplan.unplanserver.domain.recommendation.engine.EmptyTimeFinder;
import com.unplan.unplanserver.domain.recommendation.engine.QueueCard;
import com.unplan.unplanserver.domain.recommendation.engine.RecommendationMatcher;
import com.unplan.unplanserver.domain.recommendation.engine.RecommendationMatcher.MatchedCard;
import com.unplan.unplanserver.domain.recommendation.engine.TimeRange;
import com.unplan.unplanserver.domain.recommendation.engine.TimeSlot;
import com.unplan.unplanserver.domain.recommendation.entity.Recommendation;
import com.unplan.unplanserver.domain.recommendation.enums.RecommendationSourceType;
import com.unplan.unplanserver.domain.recommendation.repository.RecommendationRepository;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import com.unplan.unplanserver.domain.schedule.service.TagService;
import com.unplan.unplanserver.domain.setting.repository.SettingRepository;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.unplan.unplanserver.domain.schedule.enums.ConditionTag.RECOVERY;

/**
 * 홈/컨디션 탭 일정 추천 생성 (Notion "추천 로직" 1~4단계 조립).
 * 빈 시간 탐색 → 소요시간 필터 → 컨디션 태그 매칭 → 정렬 → 슬롯 배치 → 영속화.
 *
 * GET 시마다 이전 노출분(PENDING)을 지우고 재생성한다 — 큐 카드/핀 카드/컨디션이 수시로 바뀌므로.
 * 수락(ACCEPTED) 이력은 남긴다. '패스'(넘기기)는 서버 상태 변경 없이 프론트 페이지네이션으로 처리되며,
 * 넘긴 추천은 다음 GET 재생성에서 다시 후보로 나올 수 있다(Figma: 영구 '거절' 없음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    /** 핀 카드 앞뒤 버퍼(분) — Notion 1-4 */
    static final int BUFFER_MINUTES = 15;
    /** '최소 여유 시간' 폴백 — 회원 Setting(#83)이 없거나 빈시간 추천 기준이 꺼져 있을 때 */
    static final int DEFAULT_MIN_GAP_MINUTES = 0;
    /** 한 번에 노출하는 최대 추천 수 (PM 확정 2026-07-04: Figma는 4개지만 부담을 줄이려 3개로 축소) */
    static final int MAX_RECOMMENDATIONS = 3;

    /** 회복 수단 후보 길이 상한(분) — Notion 3-1 "최대 30분" */
    static final int RECOVERY_MEAN_MAX_MINUTES = 30;

    /** 큐카드 7일 추천 기본 탐색 범위(일) */
    static final int QUEUE_CARD_DEFAULT_RANGE_DAYS = 7;
    /** 큐카드 추천 확장 탐색 범위(일) — Figma "14일 이내로 찾아볼까요?" */
    static final int QUEUE_CARD_EXTENDED_RANGE_DAYS = 14;
    /** 온보딩 수면 패턴(sleepTimeline) 문자열 길이 = 24시간 (1문자 = 1시간, '1' = 수면) */
    static final int BIORHYTHM_TIMELINE_HOURS = 24;

    private final ScheduleService scheduleService;
    private final TagService tagService;
    private final ScheduleRepository scheduleRepository;
    private final RecommendationRepository recommendationRepository;
    private final MeasurementService measurementService;
    private final RecoverService recoverService;
    private final BiorhythmRepository biorhythmRepository;
    private final SettingRepository settingRepository;
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

        // 이전 노출분(미수락분) 정리 후 재생성 (수락 이력은 보존)
        recommendationRepository.deleteByMemberIdAndDateAndAcceptedScheduleIdIsNull(memberId, date);

        // 2. 빈 시간 탐색 — 반복 인스턴스를 포함한 핀 카드가 busy, 회원 설정(#83)의 최소 여유 시간·제외 시간대 반영
        EmptyTimeSettings settings = emptyTimeSettings(memberId);
        LocalTime windowStart = date.equals(now.toLocalDate()) ? now.toLocalTime() : LocalTime.MIDNIGHT;
        List<TimeRange> busy = scheduleService.findSchedulesWithRecurring(memberId, date).stream()
                .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                .map(s -> new TimeRange(s.getStartTime(), s.getEndTime()))
                .toList();
        List<TimeSlot> slots = emptyTimeFinder.findFreeSlots(
                date, windowStart, LocalTime.MIDNIGHT, busy, BUFFER_MINUTES,
                settings.minGapMinutes(), settings.banRanges());

        // 3. 추천 후보 큐 카드 — 완료·소요시간 미정(Notion 2-2)은 쿼리에서 제외되어 조회됨
        Map<Long, Schedule> candidateById = scheduleRepository.findActiveQueueCards(memberId).stream()
                .collect(Collectors.toMap(Schedule::getScheduleId, Function.identity()));
        List<QueueCard> cards = candidateById.values().stream()
                .map(s -> new QueueCard(s.getScheduleId(), s.getConditionTag(),
                        s.getEstimatedTime(), s.getDate(), s.getCreatedAt()))
                .toList();

        // '기력 회복'이면 회복 수단 후보(단일)를 덧붙일 수 있도록 회원의 회복 수단을 미리 조회 (설정 순서)
        List<String> recoveryMeans = current == RECOVERY
                ? recoverService.getRecoveryMeanLabels(memberId) : List.of();

        // 4. 슬롯을 시간순으로 돌며, 처음으로 후보가 나오는 빈 시간에 추천 배치
        //    (Figma: 바텀시트는 하나의 빈 시간 + 그 안의 추천 카드 페이지네이션)
        for (TimeSlot slot : slots) {
            List<QueueCard> fitting = cards.stream()
                    .filter(c -> c.estimatedMinutes() <= slot.durationMinutes())
                    .toList();
            // 우선순위 티어를 이어 채워 최대 MAX_RECOMMENDATIONS 개 (PM 확정 2026-07-04)
            List<MatchedCard> top = matcher.match(current, fitting, MAX_RECOMMENDATIONS, slot.durationMinutes());
            // '기력 회복'이면 기력회복 카드 뒤에 '회복 수단' 후보 1건을 덧붙여 총 MAX_RECOMMENDATIONS 를 채운다
            // (PM 확정 2026-07-04: 개별 수단이 아니라 하나의 후보, 수락 시 고른 수단이 제목).
            boolean addRecoveryMean = current == RECOVERY
                    && top.size() < MAX_RECOMMENDATIONS && !recoveryMeans.isEmpty();
            if (top.isEmpty() && !addRecoveryMean) continue;

            return persistAndRespond(memberId, date, current, slot, top, candidateById,
                    addRecoveryMean ? recoveryMeans : List.of());
        }

        // 어떤 빈 시간에도 넣을 후보가 없음 (오류 케이스 ①빈시간X ②빈시간O·매칭카드X)
        return new RecommendationListResponse(date, current.name(), null, List.of());
    }

    /**
     * 큐 카드 7일(확장 시 14일) 추천 시간대 생성 (Notion "GET /schedule/{scheduleId}/recommendations").
     * 대상 큐 카드를 핀 카드로 전환할 후보 시간대를, 오늘부터 {@code rangeDays} 이내에서 날짜별 1개씩 찾는다.
     *
     * @param rangeDays 탐색 범위(일). 7(기본) 또는 14(확장)
     */
    @Transactional
    public QueueCardRecommendationResult getQueueCardRecommendations(Long memberId, Long scheduleId, int rangeDays) {
        return generateQueueCardRecommendations(memberId, scheduleId, rangeDays, LocalDateTime.now());
    }

    /** now 를 주입받는 내부 진입점 (테스트 용이성) */
    @Transactional
    public QueueCardRecommendationResult generateQueueCardRecommendations(Long memberId, Long scheduleId,
                                                                          int rangeDays, LocalDateTime now) {
        // 1. 대상 큐 카드 로드 & 검증
        Schedule card = scheduleRepository.findByScheduleIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        if (!Boolean.TRUE.equals(card.getIsQueue()) || card.getStatus() == ScheduleStatus.DONE) {
            // 이미 핀 카드이거나 완료된 일정은 '핀 전환' 대상이 아니다
            throw new CustomException(ErrorCode.NOT_A_QUEUE_CARD);
        }
        Integer estimatedTime = card.getEstimatedTime();
        if (estimatedTime == null) {
            // 소요시간 미정이면 슬롯 길이 기준이 없어 탐색 불가 → 소요시간 입력 유도 (Figma "소요시간 변경")
            return QueueCardRecommendationResult.ofNoSlot(false, true);
        }

        // 이 큐 카드의 이전 노출분(미수락분) 정리 후 재생성 (수락 이력은 보존)
        recommendationRepository.deleteByMemberIdAndSourceScheduleIdAndAcceptedScheduleIdIsNull(memberId, scheduleId);

        // 2. 수면 시간대(온보딩 sleepTimeline)를 busy 로 — 미래 날짜에도 수면 중 시간대는 추천하지 않음
        List<TimeRange> sleepBusy = sleepBusyRanges(memberId);
        // 회원 설정(#83) — 제외 시간대는 매일 반복(LocalTime)이라 모든 탐색 날짜에 동일 적용
        EmptyTimeSettings settings = emptyTimeSettings(memberId);

        // 3. 날짜별 탐색: 오늘 ~ 오늘+rangeDays-1, 각 날짜의 가장 이른 '들어맞는' 빈 시간 1개
        LocalDate today = now.toLocalDate();
        List<Recommendation> saved = new ArrayList<>();
        int order = 0;
        for (int d = 0; d < rangeDays; d++) {
            LocalDate date = today.plusDays(d);
            LocalTime windowStart = date.equals(today) ? now.toLocalTime() : LocalTime.MIDNIGHT;

            List<TimeRange> busy = new ArrayList<>(sleepBusy);
            scheduleService.findSchedulesWithRecurring(memberId, date).stream()
                    .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                    .forEach(s -> busy.add(new TimeRange(s.getStartTime(), s.getEndTime())));

            List<TimeSlot> slots = emptyTimeFinder.findFreeSlots(
                    date, windowStart, LocalTime.MIDNIGHT, busy, BUFFER_MINUTES,
                    settings.minGapMinutes(), settings.banRanges());
            // 슬롯은 시각 오름차순이라 findFirst = 그날 가장 이른, 소요시간이 들어가는 슬롯
            TimeSlot pick = slots.stream()
                    .filter(s -> estimatedTime <= s.durationMinutes())
                    .findFirst().orElse(null);
            if (pick == null) continue;

            // 추천 시간 = 슬롯 시작 + 소요시간 (Notion "기존과 동일한 소요 시간"). 정확히 24:00이면 00:00 으로 wrap(=24:00 규약)
            LocalTime startTime = pick.start();
            LocalTime endTime = startTime.plusMinutes(estimatedTime);
            saved.add(recommendationRepository.save(Recommendation.builder()
                    .memberId(memberId)
                    .date(date)
                    .title(card.getTitle())
                    .startTime(startTime)
                    .endTime(endTime)
                    .conditionTag(card.getConditionTag())
                    .sourceType(RecommendationSourceType.QUEUE_CARD)
                    .sourceScheduleId(scheduleId)
                    .displayOrder(order++)
                    .build()));
        }

        // 4. 무슬롯 분기 — 7일이면 14일 확장 유도, 14일까지 없으면 소요시간 변경만 가능
        if (saved.isEmpty()) {
            boolean canExtend = rangeDays < QUEUE_CARD_EXTENDED_RANGE_DAYS;
            return QueueCardRecommendationResult.ofNoSlot(canExtend, !canExtend);
        }

        List<QueueCardRecommendationResponse.Slot> slotItems = saved.stream()
                .map(r -> new QueueCardRecommendationResponse.Slot(
                        r.getRecommendId(), r.getDate(), r.getStartTime(), r.getEndTime(), r.getDisplayOrder()))
                .toList();
        return QueueCardRecommendationResult.ofSuccess(new QueueCardRecommendationResponse(
                scheduleId, card.getTitle(), estimatedTime, rangeDays, slotItems));
    }

    /**
     * 온보딩 수면 패턴(sleepTimeline, 24자·'1'=수면)을 하루 안의 busy 시간 구간 목록으로 변환한다.
     * 인접한 수면 시각은 EmptyTimeFinder 가 병합하므로 여기선 시각별로 나눠 넣는다.
     * 하루 끝(23시)에 걸친 수면은 24:00 을 LocalTime 으로 표현할 수 없어 23:59 로 클램핑한다
     * (잔여 [23:59,24:00) 1분은 어떤 큐 카드도 들어가지 못해 무해). 패턴이 없으면 빈 목록.
     */
    /** 빈시간 추천 설정(#83) 스냅샷 — 최소 여유 시간(minGap)과 추천 제외 시간대 */
    record EmptyTimeSettings(int minGapMinutes, List<TimeRange> banRanges) {
        static final EmptyTimeSettings NONE = new EmptyTimeSettings(DEFAULT_MIN_GAP_MINUTES, List.of());
    }

    /**
     * 회원의 빈시간 추천 설정(#83)을 읽는다 (read-only 연동, 쓰기는 SettingService 소유).
     * Setting 행이 없으면(설정 화면 미진입 회원) 제약 없이 추천한다.
     * 각 항목의 on/off 플래그가 꺼져 있으면 해당 제약만 무시한다.
     */
    private EmptyTimeSettings emptyTimeSettings(Long memberId) {
        return settingRepository.findByMemberId(memberId)
                .map(s -> new EmptyTimeSettings(
                        Boolean.TRUE.equals(s.getIsEmptyTimeRecommendOn()) && s.getEmptyTimeCriteriaMinutes() != null
                                ? s.getEmptyTimeCriteriaMinutes() : DEFAULT_MIN_GAP_MINUTES,
                        Boolean.TRUE.equals(s.getIsRecommendBanTimeOn())
                                ? s.getRecommendBanTimeList().stream()
                                        .map(b -> new TimeRange(b.getStartTime(), b.getEndTime()))
                                        .toList()
                                : List.of()))
                .orElse(EmptyTimeSettings.NONE);
    }

    private List<TimeRange> sleepBusyRanges(Long memberId) {
        String timeline = biorhythmRepository.findByMemberId(memberId)
                .map(Biorhythm::getSleepTimeline).orElse(null);
        if (timeline == null || timeline.length() < BIORHYTHM_TIMELINE_HOURS) {
            return List.of();
        }
        List<TimeRange> ranges = new ArrayList<>();
        for (int hour = 0; hour < BIORHYTHM_TIMELINE_HOURS; hour++) {
            if (timeline.charAt(hour) != '1') continue;
            LocalTime start = LocalTime.of(hour, 0);
            LocalTime end = hour == 23 ? LocalTime.of(23, 59) : LocalTime.of(hour + 1, 0);
            ranges.add(new TimeRange(start, end));
        }
        return ranges;
    }

    private RecommendationListResponse persistAndRespond(Long memberId, LocalDate date, ConditionTag current,
                                                         TimeSlot slot, List<MatchedCard> top,
                                                         Map<Long, Schedule> candidateById,
                                                         List<String> recoveryMeans) {
        List<RecommendationItem> items = new ArrayList<>();
        int order = 0;
        for (; order < top.size(); order++) {
            QueueCard card = top.get(order).card();
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
                    .matchTier(top.get(order).tier())
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
                    top.get(order).tier().name(),
                    order,
                    null
            ));
        }

        // 회복 수단 후보 1건 (기력 회복 상태에서 자리가 남을 때). 제목은 아직 미선택(null) — 수락 시 사용자가 고름.
        if (!recoveryMeans.isEmpty()) {
            LocalTime startTime = slot.start();
            int length = Math.min(slot.durationMinutes(), RECOVERY_MEAN_MAX_MINUTES);
            LocalTime endTime = startTime.plusMinutes(length);

            Recommendation saved = recommendationRepository.save(Recommendation.builder()
                    .memberId(memberId)
                    .date(date)
                    .title(null)
                    .startTime(startTime)
                    .endTime(endTime)
                    .conditionTag(RECOVERY)
                    .sourceType(RecommendationSourceType.RECOVERY_MEAN)
                    .sourceScheduleId(null)
                    .displayOrder(order)
                    .build());

            items.add(new RecommendationItem(
                    saved.getRecommendId(),
                    null,
                    startTime,
                    endTime,
                    length,
                    null,
                    RECOVERY.name(),
                    RecommendationSourceType.RECOVERY_MEAN.name(),
                    null,
                    order,
                    recoveryMeans
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
     *   <li>회복 수단 추천: 원본 큐 카드가 없으므로 항상 새 일정 생성(INSERT). 제목은 사용자가 고른 회복 수단.</li>
     * </ul>
     */
    @Transactional
    public RecommendationAcceptResponse accept(Long memberId, Long recommendId, boolean keepQueueCard,
                                               String recoveryMean) {
        Recommendation rec = recommendationRepository.findByRecommendIdAndMemberId(recommendId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.RECOMMENDATION_NOT_FOUND));
        if (rec.isAccepted()) {
            throw new CustomException(ErrorCode.RECOMMENDATION_ALREADY_PROCESSED);
        }

        // 스케줄 모델은 자정을 넘기는 구간을 표현할 수 없다(start<end). 슬롯 끝이 정확히 24:00이면 추천
        // 스냅샷 end 는 00:00(=24:00 규약)으로 저장되므로, 핀 카드로는 23:59 로 클램핑해 유효 구간을 유지한다.
        LocalTime endTime = LocalTime.MIDNIGHT.equals(rec.getEndTime()) ? LocalTime.of(23, 59) : rec.getEndTime();

        Long scheduleId;
        boolean created;
        String title;
        if (rec.getSourceType() == RecommendationSourceType.QUEUE_CARD) {
            // 원본 큐 카드가 이미 삭제됐으면 전환/복제 대상이 없다.
            Schedule source = scheduleRepository.findByScheduleIdAndMemberId(rec.getSourceScheduleId(), memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
            title = source.getTitle();
            if (keepQueueCard) {
                // 큐 카드는 그대로 두고 핀 카드를 복제 생성 → 같은 일정이 큐+핀으로 공존
                Schedule pin = scheduleRepository.save(
                        pinCopyOf(memberId, source, rec.getDate(), rec.getStartTime(), endTime));
                // 개인 태그도 원본 그대로 복제본에 연결 (멤버 단위 태그라 find-or-create로 재사용됨)
                tagService.attachTags(pin, memberId, tagService.getTagNamesBySchedule(source));
                scheduleId = pin.getScheduleId();
                created = true;
            } else {
                // 큐 카드 → 핀 카드 전환 (원본 UPDATE)
                source.assignToPin(rec.getDate(), rec.getStartTime(), endTime);
                scheduleId = source.getScheduleId();
                created = false;
            }
        } else {
            // 회복 수단 → 사용자가 고른 수단이 제목. 회원이 설정한 회복 수단 중 하나여야 한다.
            String meanTitle = recoveryMean == null ? null : recoveryMean.trim();
            if (meanTitle == null || meanTitle.isBlank()
                    || !recoverService.getRecoveryMeanLabels(memberId).contains(meanTitle)) {
                throw new CustomException(ErrorCode.RECOVERY_MEAN_INVALID);
            }
            // 원본 큐 카드가 없으므로 새 일정 INSERT
            Schedule saved = scheduleRepository.save(Schedule.builder()
                    .memberId(memberId)
                    .title(meanTitle)
                    .conditionTag(rec.getConditionTag())
                    .date(rec.getDate())
                    .startTime(rec.getStartTime())
                    .endTime(endTime)
                    .isQueue(false)
                    .isRecurring(false)
                    .isConflict(false)
                    .isRemindOn(false)
                    .status(ScheduleStatus.TODO)
                    .build());
            scheduleId = saved.getScheduleId();
            created = true;
            title = meanTitle;
        }

        rec.accept(scheduleId);
        return new RecommendationAcceptResponse(
                rec.getRecommendId(), scheduleId, title,
                rec.getDate(), rec.getStartTime(), endTime,
                rec.getSourceType().name(), created);
    }

    /**
     * 큐 카드를 핀 카드로 복제한다. 제목·컨디션 태그·위치·메모·알림 설정은 원본 그대로 유지하고
     * 날짜·시간만 부여해 핀 카드로 만든다(Figma "제목/태그/위치/메모는 원래 큐 카드대로 유지").
     * 개인 태그(schedule_personal_tag) 연결은 저장 후 호출부에서 attachTags로 복제한다.
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
