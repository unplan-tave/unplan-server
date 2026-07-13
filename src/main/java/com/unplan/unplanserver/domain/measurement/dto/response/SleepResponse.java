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

    @JsonProperty("totalDurationMinutes")
    private Integer totalDurationMinutes;

    @JsonProperty("bedTime")
    private LocalDateTime bedTime;

    @JsonProperty("wakeUpTime")
    private LocalDateTime wakeUpTime;

    @JsonProperty("originalBedTime")
    private LocalDateTime originalBedTime;

    @JsonProperty("originalWakeUpTime")
    private LocalDateTime originalWakeUpTime;

    @JsonProperty("isNap")
    private Boolean isNap;

    @JsonProperty("isAllNight")
    private Boolean isAllNight;

    @JsonProperty("isContinuousSleep")
    private Boolean isContinuousSleep;

    @JsonProperty("continuousSleepGroupId")
    private String continuousSleepGroupId;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    public static SleepResponse from(Sleep sleep) {
        return SleepResponse.builder()
                .sleepId(sleep.getSleepId())
                .durationMinutes(sleep.getDurationMinutes())
                .totalDurationMinutes(resolveTotalDurationMinutes(sleep))
                .bedTime(sleep.getBedTime())
                .wakeUpTime(sleep.getWakeUpTime())
                .originalBedTime(resolveOriginalBedTime(sleep))
                .originalWakeUpTime(resolveOriginalWakeUpTime(sleep))
                .isNap(sleep.getNap())
                .isAllNight(sleep.getAllNight())
                .isContinuousSleep(sleep.isContinuousSleep())
                .continuousSleepGroupId(sleep.getContinuousSleepGroupId())
                .createdAt(sleep.getCreatedAt())
                .build();
    }

    private static Integer resolveTotalDurationMinutes(Sleep sleep) {
        return sleep.getTotalDurationMinutes() != null
                ? sleep.getTotalDurationMinutes()
                : sleep.getDurationMinutes();
    }

    private static LocalDateTime resolveOriginalBedTime(Sleep sleep) {
        return sleep.getOriginalBedTime() != null
                ? sleep.getOriginalBedTime()
                : sleep.getBedTime();
    }

    private static LocalDateTime resolveOriginalWakeUpTime(Sleep sleep) {
        return sleep.getOriginalWakeUpTime() != null
                ? sleep.getOriginalWakeUpTime()
                : sleep.getWakeUpTime();
    }
}
