package com.unplan.unplanserver.domain.measurement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "개별 수면 기록 상태 문구", example = "제시간에 푹 잤어요")
    @JsonProperty("sleepRecordComment")
    private String sleepRecordComment;

    public static SleepResponse from(Sleep sleep, String sleepRecordComment) {
        return SleepResponse.builder()
                .sleepId(sleep.getSleepId())
                .durationMinutes(sleep.getDurationMinutes())
                .totalDurationMinutes(sleep.getEffectiveTotalDurationMinutes())
                .bedTime(sleep.getBedTime())
                .wakeUpTime(sleep.getWakeUpTime())
                .originalBedTime(sleep.getEffectiveOriginalBedTime())
                .originalWakeUpTime(sleep.getEffectiveOriginalWakeUpTime())
                .isNap(sleep.getNap())
                .isAllNight(sleep.getAllNight())
                .isContinuousSleep(sleep.isContinuousSleep())
                .continuousSleepGroupId(sleep.getContinuousSleepGroupId())
                .createdAt(sleep.getCreatedAt())
                .sleepRecordComment(sleepRecordComment)
                .build();
    }
}
