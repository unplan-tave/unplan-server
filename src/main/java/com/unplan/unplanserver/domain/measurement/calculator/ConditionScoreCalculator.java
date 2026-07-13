package com.unplan.unplanserver.domain.measurement.calculator;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public final class ConditionScoreCalculator {

    private static final int MIN_RAW_SCORE = 0;
    private static final int MAX_RAW_SCORE = 6;
    private static final int MIN_PERCENT_SCORE = 0;
    private static final int MAX_PERCENT_SCORE = 100;
    private static final int MINUTES_PER_DAY = 24 * 60;

    private ConditionScoreCalculator() {
    }

    public static int calculateBodyScorePercent(Integer bodyScore) {
        validateRange(bodyScore, MIN_RAW_SCORE, MAX_RAW_SCORE, "bodyScore");
        return calculateRawScorePercent(bodyScore);
    }

    public static int calculateMindScorePercent(Integer mindScore) {
        validateRange(mindScore, MIN_RAW_SCORE, MAX_RAW_SCORE, "mindScore");
        return calculateRawScorePercent(mindScore);
    }

    public static int calculateRawScorePercent(double score) {
        validateRawScore(score, "score");
        return round(score / MAX_RAW_SCORE * MAX_PERCENT_SCORE);
    }

    public static int calculateSleepScore(
            Integer sleepAmountScore,
            Integer sleepPatternScore,
            Integer sleepStabilityScore
    ) {
        validatePercentScore(sleepAmountScore, "sleepAmountScore");
        validatePercentScore(sleepPatternScore, "sleepPatternScore");
        validatePercentScore(sleepStabilityScore, "sleepStabilityScore");

        return round(
                sleepAmountScore * 0.5
                        + sleepPatternScore * 0.3
                        + sleepStabilityScore * 0.2
        );
    }

    public static int calculateSleepAmountScore(Integer totalSleepMinutes, Integer targetSleepMinutes) {
        validateNonNegative(totalSleepMinutes, "totalSleepMinutes");
        validatePositive(targetSleepMinutes, "targetSleepMinutes");

        int diffMinutes = totalSleepMinutes - targetSleepMinutes;

        if (diffMinutes >= -30 && diffMinutes <= 120) {
            return 100;
        }
        if (diffMinutes < -180) {
            return 20;
        }
        if (diffMinutes < -120) {
            return 40;
        }
        if (diffMinutes < -60) {
            return 60;
        }
        if (diffMinutes < -30) {
            return 80;
        }
        if (diffMinutes <= 240) {
            return 80;
        }
        return 60;
    }

    public static int calculateSleepPatternScore(
            LocalTime targetBedTime,
            LocalTime targetWakeUpTime,
            LocalTime actualBedTime,
            LocalTime actualWakeUpTime
    ) {
        validateNotNull(targetBedTime, "targetBedTime");
        validateNotNull(targetWakeUpTime, "targetWakeUpTime");
        validateNotNull(actualBedTime, "actualBedTime");
        validateNotNull(actualWakeUpTime, "actualWakeUpTime");

        double patternDiffMinutes =
                (calculateCircularTimeDiffMinutes(targetBedTime, actualBedTime)
                        + calculateCircularTimeDiffMinutes(targetWakeUpTime, actualWakeUpTime)) / 2.0;

        if (patternDiffMinutes <= 30) {
            return 100;
        }
        if (patternDiffMinutes <= 60) {
            return 80;
        }
        if (patternDiffMinutes <= 120) {
            return 60;
        }
        if (patternDiffMinutes <= 180) {
            return 40;
        }
        if (patternDiffMinutes <= 240) {
            return 20;
        }
        return 0;
    }

    public static int calculateSleepStabilityScore(
            double sleepDurationDeviationMinutes,
            double wakeUpTimeDeviationMinutes
    ) {
        validateNonNegative(sleepDurationDeviationMinutes, "sleepDurationDeviationMinutes");
        validateNonNegative(wakeUpTimeDeviationMinutes, "wakeUpTimeDeviationMinutes");

        int sleepDurationDeviationScore = calculateDeviationScore(sleepDurationDeviationMinutes);
        int wakeUpTimeDeviationScore = calculateDeviationScore(wakeUpTimeDeviationMinutes);

        return round((sleepDurationDeviationScore + wakeUpTimeDeviationScore) / 2.0);
    }

    public static int calculateFinalConditionScore(
            Integer bodyScorePercent,
            Integer mindScorePercent,
            Integer sleepScore
    ) {
        validatePercentScore(bodyScorePercent, "bodyScorePercent");
        validatePercentScore(mindScorePercent, "mindScorePercent");
        validatePercentScore(sleepScore, "sleepScore");

        return round(
                bodyScorePercent * 0.4
                        + mindScorePercent * 0.4
                        + sleepScore * 0.2
        );
    }

    public static String calculateConditionLevel(Integer finalConditionScore) {
        validatePercentScore(finalConditionScore, "finalConditionScore");

        if (finalConditionScore <= 20) {
            return "회복 필요";
        }
        if (finalConditionScore <= 40) {
            return "가벼운 작업 추천";
        }
        if (finalConditionScore <= 60) {
            return "보통 강도 작업 가능";
        }
        if (finalConditionScore <= 80) {
            return "집중 가능";
        }
        return "고집중 상태";
    }

    public static String calculateConditionTag(
            Integer bodyScorePercent,
            Integer mindScorePercent,
            Integer sleepScore
    ) {
        validatePercentScore(bodyScorePercent, "bodyScorePercent");
        validatePercentScore(mindScorePercent, "mindScorePercent");
        validatePercentScore(sleepScore, "sleepScore");

        if (sleepScore < 20 && mindScorePercent < 40) {
            return "기력 회복";
        }
        if (sleepScore < 20) {
            return "긴급 처리";
        }
        if (sleepScore < 50 && bodyScorePercent < 40 && mindScorePercent < 40) {
            return "기력 회복";
        }
        if (bodyScorePercent >= 65 && mindScorePercent >= 65 && sleepScore >= 65) {
            return "핵심 작업";
        }
        if (mindScorePercent >= 65 && bodyScorePercent < 65) {
            return "두뇌 활동";
        }
        if (bodyScorePercent >= 65 && mindScorePercent < 65) {
            return "단순 노동";
        }
        return "일상 작업";
    }

    public static ConditionScoreResult calculateConditionScore(
            Integer bodyScore,
            Integer mindScore,
            Integer sleepScore
    ) {
        int bodyScorePercent = calculateBodyScorePercent(bodyScore);
        int mindScorePercent = calculateMindScorePercent(mindScore);
        validatePercentScore(sleepScore, "sleepScore");

        int finalConditionScore = calculateFinalConditionScore(
                bodyScorePercent,
                mindScorePercent,
                sleepScore
        );

        return new ConditionScoreResult(
                bodyScorePercent,
                mindScorePercent,
                sleepScore,
                finalConditionScore,
                calculateConditionLevel(finalConditionScore),
                calculateConditionTag(bodyScorePercent, mindScorePercent, sleepScore)
        );
    }

    private static long calculateCircularTimeDiffMinutes(LocalTime first, LocalTime second) {
        long diff = Math.abs(ChronoUnit.MINUTES.between(first, second));
        return Math.min(diff, MINUTES_PER_DAY - diff);
    }

    private static int calculateDeviationScore(double deviationMinutes) {
        if (deviationMinutes < 30) {
            return 100;
        }
        if (deviationMinutes < 60) {
            return 70;
        }
        if (deviationMinutes < 90) {
            return 40;
        }
        if (deviationMinutes < 120) {
            return 20;
        }
        return 0;
    }

    private static void validatePercentScore(Integer value, String name) {
        validateRange(value, MIN_PERCENT_SCORE, MAX_PERCENT_SCORE, name);
    }

    private static void validateRange(Integer value, int min, int max, String name) {
        validateNotNull(value, name);
        if (value < min || value > max) {
            throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
        }
    }

    private static void validateNonNegative(Integer value, String name) {
        validateNotNull(value, name);
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be greater than or equal to 0");
        }
    }

    private static void validatePositive(Integer value, String name) {
        validateNotNull(value, name);
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be greater than 0");
        }
    }

    private static void validateNonNegative(double value, String name) {
        if (Double.isNaN(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be greater than or equal to 0");
        }
    }

    private static void validateRawScore(double value, String name) {
        if (Double.isNaN(value) || value < MIN_RAW_SCORE || value > MAX_RAW_SCORE) {
            throw new IllegalArgumentException(name + " must be between " + MIN_RAW_SCORE + " and " + MAX_RAW_SCORE);
        }
    }

    private static void validateNotNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
    }

    private static int round(double value) {
        return (int) Math.round(value);
    }

    public record ConditionScoreResult(
            int bodyScorePercent,
            int mindScorePercent,
            int sleepScore,
            int finalConditionScore,
            String conditionLevel,
            String conditionTag
    ) {
    }
}
