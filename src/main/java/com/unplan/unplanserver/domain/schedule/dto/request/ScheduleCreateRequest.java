package com.unplan.unplanserver.domain.schedule.dto.request;

import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.domain.schedule.enums.RemindSoundType;
import com.unplan.unplanserver.domain.schedule.enums.RemindType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class ScheduleCreateRequest {

    @NotBlank
    private String title;

    @NotNull
    private ConditionTag conditionTag;

    private List<String> personalTags;

    @Schema(example = "2026-06-20")
    private LocalDate date;

    @Schema(example = "09:00")
    private LocalTime startTime;

    @Schema(example = "10:00")
    private LocalTime endTime;
    private Integer estimatedTime;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String memo;

    @NotNull
    private Boolean isRemindOn;

    private Integer remindMinutes;
    private RemindType remindType;
    private RemindSoundType remindSoundType;
    private RecurrenceRequest recurrence;

    @Getter
    @NoArgsConstructor
    public static class RecurrenceRequest {
        @NotNull
        private RecurrenceFreq freq;
        @NotNull
        private Integer interval;
        private String byDay;
        private String byMonthDay;
        @NotNull
        @Schema(example = "2026-12-31")
        private LocalDate until;

        private Integer count;
    }
}