package com.unplan.unplanserver.domain.measurement.calculator;

import com.unplan.unplanserver.domain.measurement.calculator.ConditionScoreCalculator.ConditionScoreResult;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionScoreCalculatorTest {

    @Test
    void calculateBodyScorePercent() {
        assertThat(ConditionScoreCalculator.calculateBodyScorePercent(0)).isZero();
        assertThat(ConditionScoreCalculator.calculateBodyScorePercent(3)).isEqualTo(50);
        assertThat(ConditionScoreCalculator.calculateBodyScorePercent(6)).isEqualTo(100);
    }

    @Test
    void calculateMindScorePercent() {
        assertThat(ConditionScoreCalculator.calculateMindScorePercent(0)).isZero();
        assertThat(ConditionScoreCalculator.calculateMindScorePercent(2)).isEqualTo(33);
        assertThat(ConditionScoreCalculator.calculateMindScorePercent(3)).isEqualTo(50);
        assertThat(ConditionScoreCalculator.calculateMindScorePercent(6)).isEqualTo(100);
    }

    @Test
    void calculateRawScorePercentFromAverageScore() {
        assertThat(ConditionScoreCalculator.calculateRawScorePercent(3.0)).isEqualTo(50);
        assertThat(ConditionScoreCalculator.calculateRawScorePercent(6.0)).isEqualTo(100);
        assertThat(ConditionScoreCalculator.calculateRawScorePercent(2.0)).isEqualTo(33);
        assertThat(ConditionScoreCalculator.calculateRawScorePercent(4.5)).isEqualTo(75);
    }

    @Test
    void calculateSleepScoreWithWeights() {
        int sleepScore = ConditionScoreCalculator.calculateSleepScore(100, 80, 40);

        assertThat(sleepScore).isEqualTo(82);
    }

    @Test
    void calculateSleepAmountScoreBoundaries() {
        int targetSleepMinutes = 480;

        assertThat(ConditionScoreCalculator.calculateSleepAmountScore(450, targetSleepMinutes)).isEqualTo(100);
        assertThat(ConditionScoreCalculator.calculateSleepAmountScore(420, targetSleepMinutes)).isEqualTo(80);
        assertThat(ConditionScoreCalculator.calculateSleepAmountScore(360, targetSleepMinutes)).isEqualTo(60);
        assertThat(ConditionScoreCalculator.calculateSleepAmountScore(300, targetSleepMinutes)).isEqualTo(40);
        assertThat(ConditionScoreCalculator.calculateSleepAmountScore(299, targetSleepMinutes)).isEqualTo(20);
        assertThat(ConditionScoreCalculator.calculateSleepAmountScore(600, targetSleepMinutes)).isEqualTo(100);
        assertThat(ConditionScoreCalculator.calculateSleepAmountScore(660, targetSleepMinutes)).isEqualTo(80);
        assertThat(ConditionScoreCalculator.calculateSleepAmountScore(720, targetSleepMinutes)).isEqualTo(80);
        assertThat(ConditionScoreCalculator.calculateSleepAmountScore(721, targetSleepMinutes)).isEqualTo(60);
    }

    @Test
    void calculateSleepPatternScoreBoundaries() {
        LocalTime targetBedTime = LocalTime.of(23, 0);
        LocalTime targetWakeUpTime = LocalTime.of(7, 0);

        assertThat(ConditionScoreCalculator.calculateSleepPatternScore(
                targetBedTime,
                targetWakeUpTime,
                LocalTime.of(23, 30),
                LocalTime.of(7, 30)
        )).isEqualTo(100);

        assertThat(ConditionScoreCalculator.calculateSleepPatternScore(
                targetBedTime,
                targetWakeUpTime,
                LocalTime.of(0, 0),
                LocalTime.of(8, 0)
        )).isEqualTo(80);

        assertThat(ConditionScoreCalculator.calculateSleepPatternScore(
                targetBedTime,
                targetWakeUpTime,
                LocalTime.of(1, 0),
                LocalTime.of(9, 0)
        )).isEqualTo(60);

        assertThat(ConditionScoreCalculator.calculateSleepPatternScore(
                targetBedTime,
                targetWakeUpTime,
                LocalTime.of(2, 0),
                LocalTime.of(10, 0)
        )).isEqualTo(40);

        assertThat(ConditionScoreCalculator.calculateSleepPatternScore(
                targetBedTime,
                targetWakeUpTime,
                LocalTime.of(3, 0),
                LocalTime.of(11, 0)
        )).isEqualTo(20);

        assertThat(ConditionScoreCalculator.calculateSleepPatternScore(
                targetBedTime,
                targetWakeUpTime,
                LocalTime.of(4, 0),
                LocalTime.of(12, 0)
        )).isZero();
    }

    @Test
    void calculateSleepPatternScoreUsesClosestDiffAroundMidnight() {
        assertThat(ConditionScoreCalculator.calculateSleepPatternScore(
                LocalTime.of(23, 30),
                LocalTime.of(7, 0),
                LocalTime.of(0, 30),
                LocalTime.of(7, 0)
        )).isEqualTo(100);

        assertThat(ConditionScoreCalculator.calculateSleepPatternScore(
                LocalTime.of(23, 30),
                LocalTime.of(7, 0),
                LocalTime.of(23, 0),
                LocalTime.of(7, 0)
        )).isEqualTo(100);
    }

    @Test
    void calculateSleepStabilityScore() {
        assertThat(ConditionScoreCalculator.calculateSleepStabilityScore(29.9, 29.9)).isEqualTo(100);
        assertThat(ConditionScoreCalculator.calculateSleepStabilityScore(30, 59.9)).isEqualTo(70);
        assertThat(ConditionScoreCalculator.calculateSleepStabilityScore(60, 89.9)).isEqualTo(40);
        assertThat(ConditionScoreCalculator.calculateSleepStabilityScore(90, 119.9)).isEqualTo(20);
        assertThat(ConditionScoreCalculator.calculateSleepStabilityScore(120, 120)).isZero();
        assertThat(ConditionScoreCalculator.calculateSleepStabilityScore(20, 80)).isEqualTo(70);
    }

    @Test
    void calculateFinalConditionScore() {
        int finalConditionScore = ConditionScoreCalculator.calculateFinalConditionScore(50, 100, 80);

        assertThat(finalConditionScore).isEqualTo(76);
    }

    @Test
    void calculateConditionLevelByScoreRange() {
        assertThat(ConditionScoreCalculator.calculateConditionLevel(20)).isEqualTo("회복 필요");
        assertThat(ConditionScoreCalculator.calculateConditionLevel(21)).isEqualTo("가벼운 작업 추천");
        assertThat(ConditionScoreCalculator.calculateConditionLevel(40)).isEqualTo("가벼운 작업 추천");
        assertThat(ConditionScoreCalculator.calculateConditionLevel(41)).isEqualTo("보통 강도 작업 가능");
        assertThat(ConditionScoreCalculator.calculateConditionLevel(60)).isEqualTo("보통 강도 작업 가능");
        assertThat(ConditionScoreCalculator.calculateConditionLevel(61)).isEqualTo("집중 가능");
        assertThat(ConditionScoreCalculator.calculateConditionLevel(80)).isEqualTo("집중 가능");
        assertThat(ConditionScoreCalculator.calculateConditionLevel(81)).isEqualTo("고집중 상태");
    }

    @Test
    void calculateConditionTagByPriority() {
        assertThat(ConditionScoreCalculator.calculateConditionTag(80, 30, 19)).isEqualTo("기력 회복");
        assertThat(ConditionScoreCalculator.calculateConditionTag(80, 40, 19)).isEqualTo("긴급 처리");
        assertThat(ConditionScoreCalculator.calculateConditionTag(39, 39, 49)).isEqualTo("기력 회복");
        assertThat(ConditionScoreCalculator.calculateConditionTag(65, 65, 65)).isEqualTo("핵심 작업");
        assertThat(ConditionScoreCalculator.calculateConditionTag(64, 65, 80)).isEqualTo("두뇌 활동");
        assertThat(ConditionScoreCalculator.calculateConditionTag(65, 64, 80)).isEqualTo("단순 노동");
        assertThat(ConditionScoreCalculator.calculateConditionTag(64, 64, 80)).isEqualTo("일상 작업");
    }

    @Test
    void calculateConditionScoreReturnsAggregatedResult() {
        ConditionScoreResult result = ConditionScoreCalculator.calculateConditionScore(3, 6, 80);

        assertThat(result.bodyScorePercent()).isEqualTo(50);
        assertThat(result.mindScorePercent()).isEqualTo(100);
        assertThat(result.sleepScore()).isEqualTo(80);
        assertThat(result.finalConditionScore()).isEqualTo(76);
        assertThat(result.conditionLevel()).isEqualTo("집중 가능");
        assertThat(result.conditionTag()).isEqualTo("두뇌 활동");
    }
}
