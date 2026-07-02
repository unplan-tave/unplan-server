package com.unplan.unplanserver.domain.schedule.dto.request;

import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RemindSoundType;
import com.unplan.unplanserver.domain.schedule.enums.RemindType;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class ScheduleUpdateRequest {

    private String title;
    private ConditionTag conditionTag;
    @Schema(example = "2026-06-20")
    private LocalDate date;

    @Schema(example = "09:00")
    private LocalTime startTime;

    @Schema(example = "10:00")
    private LocalTime endTime;

    // 0/음수 소요시간은 추천 소요시간 필터를 무의미하게 통과하므로 차단
    @Positive
    private Integer estimatedTime;

    private String memo;
    private ScheduleStatus status;
    private Boolean isRemindOn;

    @PositiveOrZero
    private Integer remindMinutes;

    private RemindType remindType;
    private RemindSoundType remindSoundType;
}
