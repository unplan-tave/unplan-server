package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.calculator.ConditionScoreCalculator;
import com.unplan.unplanserver.domain.measurement.calculator.ConditionScoreCalculator.ConditionScoreResult;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.AverageItem;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.ConditionRecord;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.MeasurementAverageResponse;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.SleepRecord;
import com.unplan.unplanserver.domain.measurement.entity.Condition;
import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import com.unplan.unplanserver.domain.measurement.enums.AverageGroupBy;
import com.unplan.unplanserver.domain.measurement.enums.AverageType;
import com.unplan.unplanserver.domain.measurement.repository.ConditionRepository;
import com.unplan.unplanserver.domain.measurement.repository.SleepRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.domain.onboarding.dto.response.BiorhythmResponse;
import com.unplan.unplanserver.domain.onboarding.dto.response.SleepConditionResponse;
import com.unplan.unplanserver.domain.onboarding.service.BiorhythmService;
import com.unplan.unplanserver.domain.onboarding.service.SleepConditionService;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import com.unplan.unplanserver.global.response.PageResponse;
import com.unplan.unplanserver.global.response.PagingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeasurementService {

    private static final int DEFAULT_BODY_SCORE = 3;
    private static final int DEFAULT_MIND_SCORE = 3;
    private static final int DEFAULT_SLEEP_SCORE = 70;
    private static final int DEFAULT_STABILITY_SCORE = 70;
    private static final int DEFAULT_TARGET_SLEEP_MINUTES = 480;
    private static final LocalTime DEFAULT_TARGET_BED_TIME = LocalTime.of(23, 0);
    private static final LocalTime DEFAULT_TARGET_WAKE_UP_TIME = LocalTime.of(7, 0);

    private final ConditionRepository conditionRepository;
    private final SleepRepository sleepRepository;
    private final MemberRepository memberRepository;
    private final SleepConditionService sleepConditionService;
    private final BiorhythmService biorhythmService;

    public MeasurementAverageResponse getAverageRecords(
            Long memberId,
            LocalDate from,
            LocalDate to,
            String type,
            String groupBy
    ) {
        if (from == null) {
            throw new IllegalArgumentException("from은 필수입니다.");
        }
        if (to == null) {
            throw new IllegalArgumentException("to는 필수입니다.");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from은 to보다 늦을 수 없습니다.");
        }

        AverageType averageType = parseAverageType(type);
        AverageGroupBy averageGroupBy = parseAverageGroupBy(groupBy);

        List<AveragePeriod> periods = createPeriods(from, to, averageGroupBy);
        LocalDate today = LocalDate.now();

        PreloadedMeasurementData preloadedData = preloadMeasurementData(memberId, periods, today);
        List<AverageItem> items = periods.stream()
                .map(period -> calculateAverageItem(period, averageType, today, preloadedData))
                .flatMap(List::stream)
                .toList();

        return new MeasurementAverageResponse(
                from,
                to,
                averageType.name(),
                averageGroupBy.name(),
                items
        );
    }



    public MeasurementRecordResponse getDailyRecord(
            Long memberId,
            LocalDate date
    ) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        List<Condition> conditions = conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(member, start, end)
                .stream()
                .sorted(Comparator.comparing(Condition::getMeasuredAt).reversed())
                .toList();
        List<Sleep> sleeps = sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(memberId, start, end)
                .stream()
                .sorted(Comparator.comparing(Sleep::getWakeUpTime).reversed())
                .toList();

        ConditionScoreSource conditionScoreSource = resolveConditionScoreSource(memberId, conditions, start);
        int sleepDurationMinutes = sleeps.stream()
                .mapToInt(Sleep::getDurationMinutes)
                .sum();
        int sleepScore = resolveSleepScore(memberId, sleeps, date);

        ConditionScoreResult scoreResult = ConditionScoreCalculator.calculateConditionScore(
                conditionScoreSource.bodyScore(),
                conditionScoreSource.mindScore(),
                sleepScore
        );

        return new MeasurementRecordResponse(
                date,
                scoreResult.finalConditionScore(),
                scoreResult.conditionLevel(),
                scoreResult.conditionTag(),
                scoreResult.bodyScorePercent(),
                scoreResult.mindScorePercent(),
                scoreResult.sleepScore(),
                sleepDurationMinutes,
                conditions.stream()
                        .map(this::toConditionRecord)
                        .toList(),
                sleeps.stream()
                        .map(this::toSleepRecord)
                        .toList(),
                !conditions.isEmpty(),
                !sleeps.isEmpty()
        );
    }

    private List<AverageItem> calculateAverageItem(
            AveragePeriod period,
            AverageType type,
            LocalDate today,
            PreloadedMeasurementData preloadedData
    ) {
        LocalDate calculationEnd = period.periodEnd().isAfter(today) ? today : period.periodEnd();
        if (period.periodStart().isAfter(calculationEnd)) {
            return List.of();
        }

        List<MeasurementRecordResponse> dailyRecords = new ArrayList<>();
        LocalDate current = period.periodStart();
        while (!current.isAfter(calculationEnd)) {
            if (hasAverageTargetRecords(current, type, preloadedData)) {
                dailyRecords.add(calculateDailyAverageRecordFromPreloadedData(current, preloadedData));
            }
            current = current.plusDays(1);
        }

        if (dailyRecords.isEmpty()) {
            return List.of();
        }

        int divisor = dailyRecords.size();
        Integer finalConditionScoreAverage = null;
        Integer bodyScorePercentAverage = null;
        Integer mindScorePercentAverage = null;
        Integer sleepScoreAverage = null;
        Integer sleepDurationMinutesAverage = null;

        if (type.includesCondition()) {
            finalConditionScoreAverage = average(dailyRecords.stream()
                    .mapToInt(MeasurementRecordResponse::finalConditionScore)
                    .sum(), divisor);
            bodyScorePercentAverage = average(dailyRecords.stream()
                    .mapToInt(MeasurementRecordResponse::bodyScorePercent)
                    .sum(), divisor);
            mindScorePercentAverage = average(dailyRecords.stream()
                    .mapToInt(MeasurementRecordResponse::mindScorePercent)
                    .sum(), divisor);
        }

        if (type.includesSleep()) {
            sleepScoreAverage = average(dailyRecords.stream()
                    .mapToInt(MeasurementRecordResponse::sleepScore)
                    .sum(), divisor);
            sleepDurationMinutesAverage = average(dailyRecords.stream()
                    .mapToInt(MeasurementRecordResponse::sleepDurationMinutes)
                    .sum(), divisor);
        }

        return List.of(new AverageItem(
                period.periodStart(),
                period.periodEnd(),
                period.label(),
                finalConditionScoreAverage,
                bodyScorePercentAverage,
                mindScorePercentAverage,
                sleepScoreAverage,
                sleepDurationMinutesAverage
        ));
    }

    private PreloadedMeasurementData preloadMeasurementData(
            Long memberId,
            List<AveragePeriod> periods,
            LocalDate today
    ) {
        List<AveragePeriod> calculationPeriods = periods.stream()
                .filter(period -> !period.periodStart().isAfter(today))
                .toList();

        if (calculationPeriods.isEmpty()) {
            return PreloadedMeasurementData.empty();
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        LocalDate minCalculationDate = calculationPeriods.stream()
                .map(AveragePeriod::periodStart)
                .min(LocalDate::compareTo)
                .orElse(today);
        LocalDate maxPeriodEnd = calculationPeriods.stream()
                .map(AveragePeriod::periodEnd)
                .max(LocalDate::compareTo)
                .orElse(today);
        LocalDate maxCalculationDate = maxPeriodEnd.isAfter(today) ? today : maxPeriodEnd;

        List<Condition> conditions = conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                        member,
                        minCalculationDate.minusDays(1).atStartOfDay(),
                        maxCalculationDate.plusDays(1).atStartOfDay()
                )
                .stream()
                .sorted(Comparator.comparing(Condition::getMeasuredAt))
                .toList();

        List<Sleep> sleeps = sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                        memberId,
                        minCalculationDate.minusDays(7).atStartOfDay(),
                        maxCalculationDate.plusDays(1).atStartOfDay()
                )
                .stream()
                .sorted(Comparator.comparing(Sleep::getWakeUpTime))
                .toList();

        SleepTarget sleepTarget = resolveSleepTarget(memberId);

        return new PreloadedMeasurementData(
                conditions,
                conditions.stream()
                        .collect(Collectors.groupingBy(condition -> condition.getMeasuredAt().toLocalDate())),
                sleeps,
                sleeps.stream()
                        .collect(Collectors.groupingBy(sleep -> sleep.getWakeUpTime().toLocalDate())),
                sleepTarget
        );
    }

    private MeasurementRecordResponse calculateDailyAverageRecordFromPreloadedData(
            LocalDate date,
            PreloadedMeasurementData preloadedData
    ) {
        List<Condition> conditions = preloadedData.conditionsByDate()
                .getOrDefault(date, List.of())
                .stream()
                .sorted(Comparator.comparing(Condition::getMeasuredAt))
                .toList();
        List<Sleep> sleeps = preloadedData.sleepsByWakeUpDate()
                .getOrDefault(date, List.of())
                .stream()
                .sorted(Comparator.comparing(Sleep::getWakeUpTime))
                .toList();

        ConditionPercentSource conditionPercentSource = resolveConditionPercentSourceForAverage(
                conditions,
                preloadedData.conditions(),
                date.atStartOfDay()
        );
        int sleepDurationMinutes = sleeps.stream()
                .mapToInt(Sleep::getDurationMinutes)
                .sum();
        int sleepScore = resolveSleepScoreFromPreloadedData(
                sleeps,
                date,
                preloadedData.sleepsByWakeUpDate(),
                preloadedData.sleeps(),
                preloadedData.sleepTarget()
        );

        int finalConditionScore = ConditionScoreCalculator.calculateFinalConditionScore(
                conditionPercentSource.bodyScorePercent(),
                conditionPercentSource.mindScorePercent(),
                sleepScore
        );

        return new MeasurementRecordResponse(
                date,
                finalConditionScore,
                ConditionScoreCalculator.calculateConditionLevel(finalConditionScore),
                ConditionScoreCalculator.calculateConditionTag(
                        conditionPercentSource.bodyScorePercent(),
                        conditionPercentSource.mindScorePercent(),
                        sleepScore
                ),
                conditionPercentSource.bodyScorePercent(),
                conditionPercentSource.mindScorePercent(),
                sleepScore,
                sleepDurationMinutes,
                conditions.stream()
                        .map(this::toConditionRecord)
                        .toList(),
                sleeps.stream()
                        .map(this::toSleepRecord)
                        .toList(),
                !conditions.isEmpty(),
                !sleeps.isEmpty()
        );
    }

    private boolean hasAverageTargetRecords(
            LocalDate date,
            AverageType type,
            PreloadedMeasurementData preloadedData
    ) {
        boolean hasConditions = !preloadedData.conditionsByDate()
                .getOrDefault(date, List.of())
                .isEmpty();
        boolean hasSleeps = !preloadedData.sleepsByWakeUpDate()
                .getOrDefault(date, List.of())
                .isEmpty();

        return switch (type) {
            case ALL -> hasConditions || hasSleeps;
            case CONDITION -> hasConditions;
            case SLEEP -> hasSleeps;
        };
    }

    private <T> PageResponse<T> toPageResponse(List<T> records, PageRequest pageRequest) {
        long offset = pageRequest.getOffset();

        List<T> content;
        if (offset >= records.size()) {
            content = List.of();
        } else {
            int start = (int) offset;
            int end = Math.min(start + pageRequest.getPageSize(), records.size());
            content = records.subList(start, end);
        }
        Page<T> page = new PageImpl<>(content, pageRequest, records.size());

        return PageResponse.of(page);
    }

    private ConditionScoreSource resolveConditionScoreSourceFromPreloadedData(
            List<Condition> conditions,
            List<Condition> allConditions,
            LocalDateTime dateStart
    ) {
        return conditions.stream()
                .max(Comparator.comparing(Condition::getMeasuredAt))
                .map(condition -> new ConditionScoreSource(condition.getBodyScore(), condition.getMindScore()))
                .orElseGet(() -> allConditions.stream()
                        .filter(condition -> !condition.getMeasuredAt().isBefore(dateStart.minusHours(24)))
                        .filter(condition -> condition.getMeasuredAt().isBefore(dateStart))
                        .max(Comparator.comparing(Condition::getMeasuredAt))
                        .map(condition -> new ConditionScoreSource(condition.getBodyScore(), condition.getMindScore()))
                        .orElseGet(() -> new ConditionScoreSource(DEFAULT_BODY_SCORE, DEFAULT_MIND_SCORE)));
    }

    private ConditionPercentSource resolveConditionPercentSourceForAverage(
            List<Condition> conditions,
            List<Condition> allConditions,
            LocalDateTime dateStart
    ) {
        if (!conditions.isEmpty()) {
            double bodyScoreAverage = conditions.stream()
                    .mapToInt(Condition::getBodyScore)
                    .average()
                    .orElse(DEFAULT_BODY_SCORE);
            double mindScoreAverage = conditions.stream()
                    .mapToInt(Condition::getMindScore)
                    .average()
                    .orElse(DEFAULT_MIND_SCORE);

            return new ConditionPercentSource(
                    ConditionScoreCalculator.calculateRawScorePercent(bodyScoreAverage),
                    ConditionScoreCalculator.calculateRawScorePercent(mindScoreAverage)
            );
        }

        ConditionScoreSource fallbackSource = resolveConditionScoreSourceFromPreloadedData(
                conditions,
                allConditions,
                dateStart
        );

        return new ConditionPercentSource(
                ConditionScoreCalculator.calculateBodyScorePercent(fallbackSource.bodyScore()),
                ConditionScoreCalculator.calculateMindScorePercent(fallbackSource.mindScore())
        );
    }

    private int resolveSleepScoreFromPreloadedData(
            List<Sleep> sleeps,
            LocalDate date,
            Map<LocalDate, List<Sleep>> sleepsByWakeUpDate,
            List<Sleep> allSleeps,
            SleepTarget sleepTarget
    ) {
        if (!sleeps.isEmpty()) {
            return calculateSleepScoreFromPreloadedData(
                    sleeps,
                    sleepTarget,
                    findRecentSleeps(allSleeps, date.plusDays(1).atStartOfDay())
            );
        }

        if (isPastSleepAbsenceCutoff(date, sleepTarget)) {
            return 0;
        }

        List<Sleep> previousDaySleeps = sleepsByWakeUpDate.getOrDefault(date.minusDays(1), List.of());
        if (previousDaySleeps.isEmpty()) {
            return DEFAULT_SLEEP_SCORE;
        }

        return calculateSleepScoreFromPreloadedData(
                previousDaySleeps,
                sleepTarget,
                findRecentSleeps(allSleeps, date.atStartOfDay())
        );
    }

    private int calculateSleepScoreFromPreloadedData(
            List<Sleep> sleeps,
            SleepTarget sleepTarget,
            List<Sleep> recentSleeps
    ) {
        if (hasAllNightSleep(sleeps)) {
            return 0;
        }

        int totalSleepMinutes = sleeps.stream()
                .mapToInt(Sleep::getDurationMinutes)
                .sum();
        int sleepAmountScore = ConditionScoreCalculator.calculateSleepAmountScore(
                totalSleepMinutes,
                sleepTarget.targetSleepMinutes()
        );
        int sleepPatternScore = calculateSleepPatternScore(sleeps, sleepTarget);
        int sleepStabilityScore = calculateSleepStabilityScore(recentSleeps);

        return ConditionScoreCalculator.calculateSleepScore(
                sleepAmountScore,
                sleepPatternScore,
                sleepStabilityScore
        );
    }

    private List<Sleep> findRecentSleeps(List<Sleep> allSleeps, LocalDateTime boundary) {
        LocalDateTime start = boundary.minusDays(7);
        return allSleeps.stream()
                .filter(sleep -> !sleep.getWakeUpTime().isBefore(start))
                .filter(sleep -> sleep.getWakeUpTime().isBefore(boundary))
                .sorted(Comparator.comparing(Sleep::getWakeUpTime).reversed())
                .limit(7)
                .toList();
    }

    private int average(int sum, int divisor) {
        return (int) Math.round(sum / (double) divisor);
    }

    private List<AveragePeriod> createPeriods(
            LocalDate from,
            LocalDate to,
            AverageGroupBy groupBy
    ) {
        return switch (groupBy) {
            case DAY -> createDayPeriods(from, to);
            case WEEK -> createWeekPeriods(from, to);
            case MONTH -> createMonthPeriods(from, to);
        };
    }

    private List<AveragePeriod> createDayPeriods(LocalDate from, LocalDate to) {
        List<AveragePeriod> periods = new ArrayList<>();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            periods.add(new AveragePeriod(
                    current,
                    current,
                    current.getMonthValue() + "/" + current.getDayOfMonth()
            ));
            current = current.plusDays(1);
        }
        return periods;
    }

    private List<AveragePeriod> createWeekPeriods(LocalDate from, LocalDate to) {
        List<AveragePeriod> periods = new ArrayList<>();
        LocalDate currentMonth = from.withDayOfMonth(1);
        LocalDate lastMonth = to.withDayOfMonth(1);

        while (!currentMonth.isAfter(lastMonth)) {
            LocalDate monthStart = currentMonth.withDayOfMonth(1);
            LocalDate monthEnd = currentMonth.with(TemporalAdjusters.lastDayOfMonth());
            LocalDate firstSunday = monthStart.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
            int weekIndex = 1;
            LocalDate weekStart = firstSunday;

            while (!weekStart.isAfter(monthEnd)) {
                LocalDate weekEnd = weekStart.plusDays(6);
                if (!weekEnd.isBefore(from) && !weekStart.isAfter(to)) {
                    periods.add(new AveragePeriod(
                            weekStart,
                            weekEnd,
                            currentMonth.getMonthValue() + "월 " + weekIndex + "주"
                    ));
                }
                weekStart = weekStart.plusWeeks(1);
                weekIndex++;
            }

            currentMonth = currentMonth.plusMonths(1);
        }

        return periods;
    }

    private List<AveragePeriod> createMonthPeriods(LocalDate from, LocalDate to) {
        List<AveragePeriod> periods = new ArrayList<>();
        LocalDate current = from.withDayOfMonth(1);
        LocalDate last = to.withDayOfMonth(1);

        while (!current.isAfter(last)) {
            periods.add(new AveragePeriod(
                    current.withDayOfMonth(1),
                    current.with(TemporalAdjusters.lastDayOfMonth()),
                    String.format("%d.%02d", current.getYear(), current.getMonthValue())
            ));
            current = current.plusMonths(1);
        }

        return periods;
    }

    private AverageType parseAverageType(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("type은 필수입니다.");
        }
        try {
            return AverageType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("type 값이 올바르지 않습니다.");
        }
    }

    private AverageGroupBy parseAverageGroupBy(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("groupBy는 필수입니다.");
        }
        try {
            return AverageGroupBy.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("groupBy 값이 올바르지 않습니다.");
        }
    }

    private ConditionScoreSource resolveConditionScoreSource(
            Long memberId,
            List<Condition> conditions,
            LocalDateTime dateStart
    ) {
        return conditions.stream()
                .max(Comparator.comparing(Condition::getMeasuredAt))
                .map(condition -> new ConditionScoreSource(condition.getBodyScore(), condition.getMindScore()))
                .orElseGet(() -> conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                                memberId,
                                dateStart.minusHours(24),
                                dateStart
                        )
                        .map(condition -> new ConditionScoreSource(condition.getBodyScore(), condition.getMindScore()))
                        .orElseGet(() -> new ConditionScoreSource(DEFAULT_BODY_SCORE, DEFAULT_MIND_SCORE)));
    }

    private ConditionRecord toConditionRecord(Condition condition) {
        return new ConditionRecord(
                condition.getConditionId(),
                condition.getBodyScore(),
                condition.getMindScore(),
                ConditionScoreCalculator.calculateBodyScorePercent(condition.getBodyScore()),
                ConditionScoreCalculator.calculateMindScorePercent(condition.getMindScore()),
                condition.getMeasuredAt()
        );
    }

    private SleepRecord toSleepRecord(Sleep sleep) {
        return new SleepRecord(
                sleep.getSleepId(),
                sleep.getDurationMinutes(),
                sleep.getEffectiveTotalDurationMinutes(),
                sleep.getBedTime(),
                sleep.getWakeUpTime(),
                sleep.getEffectiveOriginalBedTime(),
                sleep.getEffectiveOriginalWakeUpTime(),
                sleep.getNap(),
                sleep.getAllNight(),
                sleep.isContinuousSleep(),
                sleep.getContinuousSleepGroupId(),
                sleep.getCreatedAt()
        );
    }

    private int resolveSleepScore(Long memberId, List<Sleep> sleeps, LocalDate date) {
        if (!sleeps.isEmpty()) {
            SleepTarget sleepTarget = resolveSleepTarget(memberId);
            return calculateSleepScore(memberId, sleeps, date.plusDays(1).atStartOfDay(), sleepTarget);
        }

        SleepTimelineTarget timelineTarget = resolveSleepTimelineTarget(memberId);
        if (isPastSleepAbsenceCutoff(date, timelineTarget)) {
            return 0;
        }

        LocalDateTime previousDayStart = date.minusDays(1).atStartOfDay();
        LocalDateTime previousDayEnd = date.atStartOfDay();
        List<Sleep> previousDaySleeps = sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                previousDayStart,
                previousDayEnd
        );

        if (previousDaySleeps.isEmpty()) {
            return DEFAULT_SLEEP_SCORE;
        }

        SleepTarget sleepTarget = resolveSleepTarget(memberId);
        return calculateSleepScore(memberId, previousDaySleeps, previousDayEnd, sleepTarget);
    }

    private int calculateSleepScore(
            Long memberId,
            List<Sleep> sleeps,
            LocalDateTime recentSleepBoundary,
            SleepTarget sleepTarget
    ) {
        if (hasAllNightSleep(sleeps)) {
            return 0;
        }

        int totalSleepMinutes = sleeps.stream()
                .mapToInt(Sleep::getDurationMinutes)
                .sum();
        int sleepAmountScore = ConditionScoreCalculator.calculateSleepAmountScore(
                totalSleepMinutes,
                sleepTarget.targetSleepMinutes()
        );
        int sleepPatternScore = calculateSleepPatternScore(sleeps, sleepTarget);
        int sleepStabilityScore = calculateSleepStabilityScore(memberId, recentSleepBoundary);

        return ConditionScoreCalculator.calculateSleepScore(
                sleepAmountScore,
                sleepPatternScore,
                sleepStabilityScore
        );
    }

    private boolean hasAllNightSleep(List<Sleep> sleeps) {
        return sleeps.stream()
                .anyMatch(sleep -> sleep.getDurationMinutes() == 0 || Boolean.TRUE.equals(sleep.getAllNight()));
    }

    private SleepTarget resolveSleepTarget(Long memberId) {
        int targetSleepMinutes = resolveTargetSleepMinutes(memberId);
        SleepTimelineTarget timelineTarget = resolveSleepTimelineTarget(memberId);

        return new SleepTarget(
                targetSleepMinutes,
                timelineTarget.targetBedTime(),
                timelineTarget.targetWakeUpTime()
        );
    }

    private int resolveTargetSleepMinutes(Long memberId) {
        try {
            SleepConditionResponse sleepCondition = sleepConditionService.getSleepCondition(memberId);
            if (sleepCondition != null && sleepCondition.targetDuration() != null) {
                return sleepCondition.targetDuration();
            }
        } catch (CustomException e) {

        }

        return DEFAULT_TARGET_SLEEP_MINUTES;
    }

    private SleepTimelineTarget resolveSleepTimelineTarget(Long memberId) {
        try {
            BiorhythmResponse.GetBiorhythm biorhythm = biorhythmService.getBiorhythm(memberId);
            if (biorhythm == null) {
                return new SleepTimelineTarget(DEFAULT_TARGET_BED_TIME, DEFAULT_TARGET_WAKE_UP_TIME);
            }
            String sleepTimeline = biorhythm.sleepTimeline();

            if (sleepTimeline != null
                    && sleepTimeline.length() == 24
                    && sleepTimeline.contains("1")) {
                int bedHour = findSleepStartHour(sleepTimeline);
                int wakeHour = findSleepEndHour(sleepTimeline, bedHour);

                return new SleepTimelineTarget(
                        LocalTime.of(bedHour, 0),
                        LocalTime.of(wakeHour, 0)
                );
            }
        } catch (CustomException e) {

        }

        return new SleepTimelineTarget(DEFAULT_TARGET_BED_TIME, DEFAULT_TARGET_WAKE_UP_TIME);
    }

    private boolean isPastSleepAbsenceCutoff(LocalDate date, SleepTarget sleepTarget) {
        LocalDateTime cutoff = date.atTime(sleepTarget.targetWakeUpTime()).plusHours(6);
        return LocalDateTime.now().isAfter(cutoff);
    }

    private boolean isPastSleepAbsenceCutoff(LocalDate date, SleepTimelineTarget sleepTimelineTarget) {
        LocalDateTime cutoff = date.atTime(sleepTimelineTarget.targetWakeUpTime()).plusHours(6);
        return LocalDateTime.now().isAfter(cutoff);
    }

    private int findSleepStartHour(String sleepTimeline) {
        for (int i = 0; i < sleepTimeline.length(); i++) {
            char previous = sleepTimeline.charAt((i + sleepTimeline.length() - 1) % sleepTimeline.length());
            char current = sleepTimeline.charAt(i);

            if (previous == '0' && current == '1') {
                return i;
            }
        }

        return 0;
    }

    private int findSleepEndHour(String sleepTimeline, int bedHour) {
        for (int offset = 1; offset <= sleepTimeline.length(); offset++) {
            int index = (bedHour + offset) % sleepTimeline.length();
            if (sleepTimeline.charAt(index) == '0') {
                return index;
            }
        }

        return bedHour;
    }

    private int calculateSleepPatternScore(List<Sleep> sleeps, SleepTarget sleepTarget) {
        List<Sleep> nightSleeps = sleeps.stream()
                .filter(sleep -> !sleep.getNap())
                .toList();

        if (nightSleeps.isEmpty()) {
            return 0;
        }

        double averageScore = nightSleeps.stream()
                .mapToInt(sleep -> ConditionScoreCalculator.calculateSleepPatternScore(
                        sleepTarget.targetBedTime(),
                        sleepTarget.targetWakeUpTime(),
                        sleep.getBedTime().toLocalTime(),
                        sleep.getWakeUpTime().toLocalTime()
                ))
                .average()
                .orElse(0);

        return (int) Math.round(averageScore);
    }

    private int calculateSleepStabilityScore(Long memberId, LocalDateTime recentSleepBoundary) {
        List<Sleep> recentSleeps = sleepRepository.findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
                memberId,
                recentSleepBoundary
        );

        return calculateSleepStabilityScore(findRecentSleeps(recentSleeps, recentSleepBoundary));
    }

    private int calculateSleepStabilityScore(List<Sleep> recentSleeps) {
        if (recentSleeps.size() < 2) {
            return DEFAULT_STABILITY_SCORE;
        }

        double durationDeviation = calculateStandardDeviation(
                recentSleeps.stream()
                        .mapToDouble(Sleep::getDurationMinutes)
                        .toArray()
        );
        double wakeUpTimeDeviation = calculateStandardDeviation(
                recentSleeps.stream()
                        .mapToDouble(sleep -> sleep.getWakeUpTime().toLocalTime().toSecondOfDay() / 60.0)
                        .toArray()
        );

        return ConditionScoreCalculator.calculateSleepStabilityScore(durationDeviation, wakeUpTimeDeviation);
    }

    private double calculateStandardDeviation(double[] values) {
        double average = 0;
        for (double value : values) {
            average += value;
        }
        average /= values.length;

        double variance = 0;
        for (double value : values) {
            variance += Math.pow(value - average, 2);
        }
        variance /= values.length;

        return Math.sqrt(variance);
    }

    private record ConditionScoreSource(
            int bodyScore,
            int mindScore
    ) {
    }

    private record ConditionPercentSource(
            int bodyScorePercent,
            int mindScorePercent
    ) {
    }

    private record SleepTarget(
            int targetSleepMinutes,
            LocalTime targetBedTime,
            LocalTime targetWakeUpTime
    ) {
    }

    private record SleepTimelineTarget(
            LocalTime targetBedTime,
            LocalTime targetWakeUpTime
    ) {
    }

    private record PreloadedMeasurementData(
            List<Condition> conditions,
            Map<LocalDate, List<Condition>> conditionsByDate,
            List<Sleep> sleeps,
            Map<LocalDate, List<Sleep>> sleepsByWakeUpDate,
            SleepTarget sleepTarget
    ) {

        private static PreloadedMeasurementData empty() {
            return new PreloadedMeasurementData(
                    List.of(),
                    Map.of(),
                    List.of(),
                    Map.of(),
                    new SleepTarget(DEFAULT_TARGET_SLEEP_MINUTES, DEFAULT_TARGET_BED_TIME, DEFAULT_TARGET_WAKE_UP_TIME)
            );
        }
    }

    private record AveragePeriod(
            LocalDate periodStart,
            LocalDate periodEnd,
            String label
    ) {
    }

}
