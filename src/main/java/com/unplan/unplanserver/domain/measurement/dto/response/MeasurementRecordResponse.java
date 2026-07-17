package com.unplan.unplanserver.domain.measurement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MeasurementRecordResponse(
        LocalDate date,
        int finalConditionScore,
        String conditionLevel,
        String conditionTag,
        int bodyScorePercent,
        int mindScorePercent,
        int sleepScore,
        int sleepDurationMinutes,
        List<ConditionRecord> conditions,
        List<SleepRecord> sleeps,
        Boolean isEnergyRecorded,
        Boolean isSleepRecorded
) {
    public record ConditionRecord(
            Long conditionId,
            Integer bodyScore,
            Integer mindScore,
            int bodyScorePercent,
            int mindScorePercent,
            LocalDateTime dateTime
    ) {
    }

    public record SleepRecord(
            Long sleepId,
            Integer durationMinutes,
            Integer totalDurationMinutes,
            LocalDateTime bedTime,
            LocalDateTime wakeUpTime,
            LocalDateTime originalBedTime,
            LocalDateTime originalWakeUpTime,
            @JsonProperty("isNap")
            Boolean isNap,
            @JsonProperty("isAllNight")
            Boolean isAllNight,
            @JsonProperty("isContinuousSleep")
            Boolean isContinuousSleep,
            String continuousSleepGroupId,
            LocalDateTime createdAt,
            @Schema(description = "개별 수면 기록 상태 문구", example = "제시간에 푹 잤어요")
            String sleepRecordComment
    ) {
        public SleepRecord(
                Long sleepId,
                Integer durationMinutes,
                Integer totalDurationMinutes,
                LocalDateTime bedTime,
                LocalDateTime wakeUpTime,
                LocalDateTime originalBedTime,
                LocalDateTime originalWakeUpTime,
                Boolean isNap,
                Boolean isAllNight,
                Boolean isContinuousSleep,
                String continuousSleepGroupId,
                LocalDateTime createdAt
        ) {
            this(sleepId, durationMinutes, totalDurationMinutes, bedTime, wakeUpTime,
                    originalBedTime, originalWakeUpTime, isNap, isAllNight, isContinuousSleep,
                    continuousSleepGroupId, createdAt, null);
        }
    }

    public record MeasurementAverageResponse(
            LocalDate from,
            LocalDate to,
            String type,
            String groupBy,
            List<AverageItem> items
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AverageItem(
            LocalDate periodStart,
            LocalDate periodEnd,
            String label,
            Integer finalConditionScoreAverage,
            Integer bodyScorePercentAverage,
            Integer mindScorePercentAverage,
            Integer sleepScoreAverage,
            Integer sleepDurationMinutesAverage,
            @Schema(description = "Body 평균 점수 기반 상태 문구", example = "에너지가 넘쳐요!")
            String bodyComment,
            @Schema(description = "Mind 평균 점수 기반 상태 문구", example = "집중력이 좋아요!")
            String mindComment,
            @Schema(description = "평균 수면 시간 기반 상태 문구", example = "수면 시간 보통")
            String sleepComment
    ) {
    }
}
