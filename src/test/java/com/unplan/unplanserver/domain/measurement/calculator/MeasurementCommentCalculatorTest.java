package com.unplan.unplanserver.domain.measurement.calculator;

import com.unplan.unplanserver.domain.measurement.calculator.MeasurementCommentCalculator.SleepConditionSettings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class MeasurementCommentCalculatorTest {

    private static final SleepConditionSettings SETTINGS =
            new SleepConditionSettings(480, 360, 420, 600);
    private static final LocalDateTime BED_TIME = LocalDateTime.of(2026, 7, 10, 23, 0);

    @ParameterizedTest
    @CsvSource({
            "95, 에너지 최상", "75, 에너지 높음", "40, 에너지 보통",
            "20, 에너지 부족", "19, 에너지 매우 부족"
    })
    void calculatesAverageEnergyComment(int score, String expected) {
        assertThat(MeasurementCommentCalculator.calculateAverageEnergyComment(score)).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "480, 수면 시간 최상", "300, 수면 시간 매우 부족",
            "390, 수면 시간 부족", "500, 수면 시간 적정", "650, 수면 시간 과다"
    })
    void calculatesAverageSleepComment(int duration, String expected) {
        assertThat(MeasurementCommentCalculator.calculateAverageSleepComment(duration, SETTINGS))
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "60, 가볍게 충전하는 시간을 가졌어요",
            "61, 충분히 쉬는 시간을 가졌어요",
            "90, 충분히 쉬는 시간을 가졌어요",
            "91, 밤잠에 영향을 줄 수도 있어요"
    })
    void calculatesNapComment(int duration, String expected) {
        assertThat(comment(false, true, false, duration, BED_TIME)).isEqualTo(expected);
    }

    @Test
    void calculatesContinuousSleepComment() {
        assertThat(MeasurementCommentCalculator.calculateSleepRecordComment(
                false, false, true, 1440, BED_TIME,
                LocalDateTime.of(2026, 7, 10, 23, 0),
                LocalDateTime.of(2026, 7, 12, 7, 30),
                SETTINGS, LocalTime.of(23, 0)
        )).isEqualTo("10일~12일까지의 연속수면이에요");
    }

    @ParameterizedTest
    @CsvSource({
            "300, 23:00, 수면량이 매우 부족해요",
            "300, 21:00, 평소보다 일찍 잤지만 잠이 매우 부족해요",
            "300, 01:00, '평소보다 늦게 잤고, 잠이 매우 부족해요'",
            "390, 23:00, 잠이 조금 부족해요",
            "390, 21:00, 평소보다 일찍 잤지만 잠이 조금 부족해요",
            "390, 01:00, '평소보다 늦게 잤고, 잠이 조금 부족해요'",
            "500, 23:00, 충분히 잘 잤어요",
            "500, 21:00, '평소보다 일찍 잤고, 수면량이 충분해요'",
            "500, 01:00, 평소보다 늦게 잤지만 수면량이 충분해요",
            "650, 23:00, 잠을 너무 많이 잤어요",
            "650, 21:00, '평소보다 일찍 잤고, 수면량이 과다해요'",
            "650, 01:00, 평소보다 늦게 잤지만 수면량이 과다해요",
            "480, 23:00, 목표한 만큼 잤어요",
            "480, 21:00, '평소보다 일찍 잤고, 목표한 만큼 잠들었어요'",
            "480, 01:00, 평소보다 늦게 잤지만 목표한 만큼 잠들었어요"
    })
    void calculatesGeneralSleepComment(int duration, LocalTime bedTime, String expected) {
        assertThat(comment(
                false, false, false, duration,
                LocalDateTime.of(2026, 7, 10, bedTime.getHour(), bedTime.getMinute())
        )).isEqualTo(expected);
    }

    @Test
    void keepsAllNightComment() {
        assertThat(comment(true, true, true, 0, BED_TIME)).isEqualTo("밤샘으로 기록됐어요");
    }

    private String comment(boolean allNight, boolean nap, boolean continuous, int duration, LocalDateTime bedTime) {
        return MeasurementCommentCalculator.calculateSleepRecordComment(
                allNight, nap, continuous, duration, bedTime, bedTime,
                bedTime.plusMinutes(duration), SETTINGS, LocalTime.of(23, 0)
        );
    }
}
