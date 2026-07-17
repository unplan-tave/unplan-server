package com.unplan.unplanserver.domain.measurement.calculator;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementCommentCalculatorTest {

    @ParameterizedTest
    @CsvSource({
            "70, 에너지가 넘쳐요!",
            "69, 에너지 보통",
            "40, 에너지 보통",
            "39, 에너지 부족"
    })
    void calculatesBodyComment(int score, String expected) {
        assertThat(MeasurementCommentCalculator.calculateBodyComment(score)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "70, 집중력이 좋아요!",
            "69, 집중력 보통",
            "40, 집중력 보통",
            "39, 에너지 부족"
    })
    void calculatesMindComment(int score, String expected) {
        assertThat(MeasurementCommentCalculator.calculateMindComment(score)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "0, 480, 수면 기록이 없어요",
            "600, 480, 수면 시간이 길어요",
            "599, 480, 수면 시간 보통",
            "420, 480, 수면 시간이 부족해요",
            "421, 480, 수면 시간 보통"
    })
    void calculatesAverageSleepComment(
            int durationMinutes,
            int targetSleepMinutes,
            String expected
    ) {
        assertThat(MeasurementCommentCalculator.calculateSleepComment(
                durationMinutes,
                targetSleepMinutes
        )).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "true, true, 600, 480, 밤샘으로 기록됐어요",
            "false, true, 600, 480, 가볍게 충전하는 시간을 가졌어요",
            "false, false, 600, 480, 과다 수면이에요",
            "false, false, 420, 480, 조금 일찍 일어났어요",
            "false, false, 421, 480, 제시간에 푹 잤어요"
    })
    void calculatesSleepRecordComment(
            boolean isAllNight,
            boolean isNap,
            int durationMinutes,
            int targetSleepMinutes,
            String expected
    ) {
        assertThat(MeasurementCommentCalculator.calculateSleepRecordComment(
                isAllNight,
                isNap,
                durationMinutes,
                targetSleepMinutes
        )).isEqualTo(expected);
    }
}
