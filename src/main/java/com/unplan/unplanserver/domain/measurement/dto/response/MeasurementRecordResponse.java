package com.unplan.unplanserver.domain.measurement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

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
        List<SleepRecord> sleeps
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
            LocalDateTime bedTime,
            LocalDateTime wakeUpTime,
            @JsonProperty("isNap")
            Boolean isNap,
            LocalDateTime createdAt
    ) {
    }
}
