package com.unplan.unplanserver.domain.schedule.dto.response;

import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;

import java.time.LocalDate;

public record DailyMessageResponseDto(
        LocalDate date,
        ConditionTag condition,
        String message,
        Boolean isEnergyRecorded,
        Boolean isSleepRecorded
) {
}
