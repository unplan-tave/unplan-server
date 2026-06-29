package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.calculator.ConditionScoreCalculator;
import com.unplan.unplanserver.domain.measurement.calculator.ConditionScoreCalculator.ConditionScoreResult;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.ConditionRecord;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.SleepRecord;
import com.unplan.unplanserver.domain.measurement.entity.Condition;
import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import com.unplan.unplanserver.domain.measurement.repository.ConditionRepository;
import com.unplan.unplanserver.domain.measurement.repository.SleepRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeasurementService {

    private static final int DEFAULT_BODY_SCORE = 3;
    private static final int DEFAULT_MIND_SCORE = 3;
    private static final int DEFAULT_SLEEP_SCORE = 70;
    private static final int DEFAULT_STABILITY_SCORE = 70;

    private final ConditionRepository conditionRepository;
    private final SleepRepository sleepRepository;
    private final MemberRepository memberRepository;

    public MeasurementRecordResponse getDailyRecord(Long memberId, LocalDate date) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        LocalDateTime inclusiveEnd = end.minusNanos(1);

        List<Condition> conditions = conditionRepository.findAllByMemberAndMeasuredAtBetween(member, start, inclusiveEnd)
                .stream()
                .sorted(Comparator.comparing(Condition::getMeasuredAt))
                .toList();
        List<Sleep> sleeps = sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(memberId, start, inclusiveEnd)
                .stream()
                .sorted(Comparator.comparing(Sleep::getWakeUpTime))
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
                        .toList()
        );
    }

    private ConditionScoreSource resolveConditionScoreSource(
            Long memberId,
            List<Condition> conditions,
            LocalDateTime dateStart
    ) {
        return conditions.stream()
                .max(Comparator.comparing(Condition::getMeasuredAt))
                .map(condition -> new ConditionScoreSource(condition.getBodyScore(), condition.getMindScore()))
                .orElseGet(() -> conditionRepository.findTopByMemberMemberIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                                memberId,
                                dateStart.minusHours(24),
                                dateStart.minusNanos(1)
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
                sleep.getBedTime(),
                sleep.getWakeUpTime(),
                sleep.getNap(),
                sleep.getCreatedAt()
        );
    }

    private int resolveSleepScore(Long memberId, List<Sleep> sleeps, LocalDate date) {
        if (!sleeps.isEmpty()) {
            return calculateSleepScore(memberId, sleeps, date.plusDays(1).atStartOfDay());
        }

        LocalDateTime previousDayStart = date.minusDays(1).atStartOfDay();
        LocalDateTime previousDayEnd = date.atStartOfDay();
        List<Sleep> previousDaySleeps = sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                previousDayStart,
                previousDayEnd.minusNanos(1)
        );

        if (previousDaySleeps.isEmpty()) {
            return DEFAULT_SLEEP_SCORE;
        }

        return calculateSleepScore(memberId, previousDaySleeps, previousDayEnd);
    }

    private int calculateSleepScore(Long memberId, List<Sleep> sleeps, LocalDateTime recentSleepBoundary) {
        if (sleeps.stream().anyMatch(sleep -> sleep.getDurationMinutes() == 0)) {
            return 0;
        }

        SleepTarget sleepTarget = resolveSleepTarget(memberId);
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

    private SleepTarget resolveSleepTarget(Long memberId) {
        // TODO: Replace temporary defaults with user onboarding/settings values.
        return new SleepTarget(
                480,
                LocalTime.of(23, 0),
                LocalTime.of(7, 0)
        );
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

        // Recent stability needs at least 2 sleep records to calculate deviation.
        // Until enough data exists, use the neutral default score.
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

    private record SleepTarget(
            int targetSleepMinutes,
            LocalTime targetBedTime,
            LocalTime targetWakeUpTime
    ) {
    }
}
