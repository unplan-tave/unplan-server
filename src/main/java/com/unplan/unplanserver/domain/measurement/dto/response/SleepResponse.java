package com.unplan.unplanserver.domain.measurement.dto.response;

import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SleepResponse {

    private Long sleepId;
    private Integer durationMinutes;
    private LocalDateTime bedTime;
    private LocalDateTime wakeUpTime;
    private Boolean isNap;
    private LocalDateTime createdAt;

    public static SleepResponse from(Sleep sleep) {
        return SleepResponse.builder()
                .sleepId(sleep.getSleepId())
                .durationMinutes(sleep.getDurationMinutes())
                .bedTime(sleep.getBedTime())
                .wakeUpTime(sleep.getWakeUpTime())
                .isNap(sleep.getNap())
                .createdAt(sleep.getCreatedAt())
                .build();
    }
}