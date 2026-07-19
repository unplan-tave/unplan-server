package com.unplan.unplanserver.domain.measurement.calculator;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public final class MeasurementCommentCalculator {

    private static final int TARGET_TOLERANCE_MINUTES = 15;
    private static final int BED_TIME_THRESHOLD_MINUTES = 90;

    private MeasurementCommentCalculator() {
    }

    public static String calculateBodyComment(int score) {
        if (score >= 70) return "에너지가 넘쳐요!";
        if (score >= 40) return "에너지 보통";
        return "에너지 부족";
    }

    public static String calculateMindComment(int score) {
        if (score >= 70) return "집중력이 좋아요!";
        if (score >= 40) return "집중력 보통";
        return "에너지 부족";
    }

    public static String calculateAverageEnergyComment(int score) {
        if (score >= 95) return "에너지 최상";
        if (score >= 75) return "에너지 높음";
        if (score >= 40) return "에너지 보통";
        if (score >= 20) return "에너지 부족";
        return "에너지 매우 부족";
    }

    public static String calculateAverageSleepComment(int duration, SleepConditionSettings settings) {
        return switch (calculateAmountLevel(duration, settings, false)) {
            case TARGET -> "수면 시간 최상";
            case DANGER -> "수면 시간 매우 부족";
            case LACK -> "수면 시간 부족";
            case OPTIMAL -> "수면 시간 적정";
            case EXCESS -> "수면 시간 과다";
        };
    }

    public static String calculateSleepRecordComment(
            Boolean isAllNight,
            Boolean isNap,
            boolean isContinuousSleep,
            int durationMinutes,
            LocalDateTime bedTime,
            LocalDateTime originalBedTime,
            LocalDateTime originalWakeUpTime,
            SleepConditionSettings settings,
            LocalTime targetBedTime
    ) {
        if (Boolean.TRUE.equals(isAllNight)) return "밤샘으로 기록됐어요";
        if (Boolean.TRUE.equals(isNap)) {
            if (durationMinutes <= 60) return "가볍게 충전하는 시간을 가졌어요";
            if (durationMinutes <= 90) return "충분히 쉬는 시간을 가졌어요";
            return "밤잠에 영향을 줄 수도 있어요";
        }
        if (isContinuousSleep) {
            return originalBedTime.getDayOfMonth() + "일~"
                    + originalWakeUpTime.getDayOfMonth() + "일까지의 연속수면이에요";
        }

        AmountLevel amount = calculateAmountLevel(durationMinutes, settings, true);
        PatternLevel pattern = calculatePatternLevel(bedTime.toLocalTime(), targetBedTime);
        return switch (amount) {
            case DANGER -> switch (pattern) {
                case NORMAL -> "수면이 매우 부족해요";
                case EARLY -> "일찍 잠들었지만 수면이 매우 부족해요";
                case LATE -> "늦게 잠들어 수면이 매우 부족해요";
            };
            case LACK -> switch (pattern) {
                case NORMAL -> "잠이 조금 부족해요";
                case EARLY -> "일찍 잠들었지만 잠이 조금 부족해요";
                case LATE -> "늦게 잠들어 잠이 조금 부족해요";
            };
            case OPTIMAL -> switch (pattern) {
                case NORMAL -> "충분히 잘 잤어요";
                case EARLY -> "일찍 잠들어 충분히 쉬었어요";
                case LATE -> "늦게 잠들었지만 충분히 쉬었어요";
            };
            case EXCESS -> switch (pattern) {
                case NORMAL -> "오래 푹 잠들었어요";
                case EARLY -> "일찍 잠들어 오래 쉬었어요";
                case LATE -> "늦게 잠들었지만 오래 쉬었어요";
            };
            case TARGET -> switch (pattern) {
                case NORMAL -> "목표한 만큼 잤어요";
                case EARLY -> "일찍 잠들어 목표한 만큼 쉬었어요";
                case LATE -> "늦게 잠들었지만 목표한 만큼 쉬었어요";
            };
        };
    }

    public static String calculateSleepRecordComment(
            Boolean isAllNight,
            Boolean isNap,
            int durationMinutes,
            int targetSleepMinutes
    ) {
        LocalDateTime fallbackBedTime = LocalDateTime.of(2000, 1, 1, 23, 0);
        return calculateSleepRecordComment(
                isAllNight,
                isNap,
                false,
                durationMinutes,
                fallbackBedTime,
                fallbackBedTime,
                fallbackBedTime.plusMinutes(durationMinutes),
                new SleepConditionSettings(targetSleepMinutes, 360, 420, 600),
                LocalTime.of(23, 0)
        );
    }

    private static AmountLevel calculateAmountLevel(
            int duration,
            SleepConditionSettings settings,
            boolean useTolerance
    ) {
        if (useTolerance
                ? Math.abs(duration - settings.targetSleepMinutes()) <= TARGET_TOLERANCE_MINUTES
                : duration == settings.targetSleepMinutes()) {
            return AmountLevel.TARGET;
        }
        if (duration <= settings.dangerThreshold()) return AmountLevel.DANGER;
        if (duration <= settings.lackThreshold()) return AmountLevel.LACK;
        if (duration <= settings.optimalThreshold()) return AmountLevel.OPTIMAL;
        return AmountLevel.EXCESS;
    }

    private static PatternLevel calculatePatternLevel(LocalTime actual, LocalTime target) {
        long difference = ChronoUnit.MINUTES.between(target, actual);
        if (difference > 720) difference -= 1440;
        if (difference < -720) difference += 1440;
        if (difference < -BED_TIME_THRESHOLD_MINUTES) return PatternLevel.EARLY;
        if (difference > BED_TIME_THRESHOLD_MINUTES) return PatternLevel.LATE;
        return PatternLevel.NORMAL;
    }

    public record SleepConditionSettings(
            int targetSleepMinutes,
            int dangerThreshold,
            int lackThreshold,
            int optimalThreshold
    ) {
    }

    private enum AmountLevel { DANGER, LACK, OPTIMAL, EXCESS, TARGET }
    private enum PatternLevel { NORMAL, EARLY, LATE }
}
