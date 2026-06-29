package com.unplan.unplanserver.domain.measurement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SleepResponse {

    @JsonProperty("sleepId")
    private Long sleepId;

    @JsonProperty("durationMinutes")
    private Integer durationMinutes;

    @JsonProperty("bedTime")
    private LocalDateTime bedTime;

    @JsonProperty("wakeUpTime")
    private LocalDateTime wakeUpTime;

    @JsonProperty("isNap")
    private Boolean isNap;

    @JsonProperty("createdAt")
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
