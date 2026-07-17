package com.unplan.unplanserver.domain.measurement.calculator;

public final class MeasurementCommentCalculator {

    private MeasurementCommentCalculator() {
    }

    public static String calculateBodyComment(int bodyScorePercent) {
        if (bodyScorePercent >= 70) {
            return "에너지가 넘쳐요!";
        }
        if (bodyScorePercent >= 40) {
            return "에너지 보통";
        }
        return "에너지 부족";
    }

    public static String calculateMindComment(int mindScorePercent) {
        if (mindScorePercent >= 70) {
            return "집중력이 좋아요!";
        }
        if (mindScorePercent >= 40) {
            return "집중력 보통";
        }
        return "에너지 부족";
    }

    public static String calculateSleepComment(int sleepDurationMinutes, int targetSleepMinutes) {
        if (sleepDurationMinutes == 0) {
            return "수면 기록이 없어요";
        }
        if (sleepDurationMinutes >= targetSleepMinutes + 120) {
            return "수면 시간이 길어요";
        }
        if (sleepDurationMinutes <= targetSleepMinutes - 60) {
            return "수면 시간이 부족해요";
        }
        return "수면 시간 보통";
    }

    public static String calculateSleepRecordComment(
            Boolean isAllNight,
            Boolean isNap,
            int durationMinutes,
            int targetSleepMinutes
    ) {
        if (Boolean.TRUE.equals(isAllNight)) {
            return "밤샘으로 기록됐어요";
        }
        if (Boolean.TRUE.equals(isNap)) {
            return "가볍게 충전하는 시간을 가졌어요";
        }
        if (durationMinutes >= targetSleepMinutes + 120) {
            return "과다 수면이에요";
        }
        if (durationMinutes <= targetSleepMinutes - 60) {
            return "조금 일찍 일어났어요";
        }
        return "제시간에 푹 잤어요";
    }
}
