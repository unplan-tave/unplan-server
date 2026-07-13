package com.unplan.unplanserver.domain.measurement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.unplan.unplanserver.global.response.PageResponse;

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
        PageResponse<ConditionRecord> conditions,
        PageResponse<SleepRecord> sleeps
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
            LocalDateTime createdAt
    ) {
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
            Integer sleepDurationMinutesAverage
    ) {
    }
}
