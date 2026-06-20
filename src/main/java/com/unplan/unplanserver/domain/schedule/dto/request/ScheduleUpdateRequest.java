package com.unplan.unplanserver.domain.schedule.dto.request;

import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RemindSoundType;
import com.unplan.unplanserver.domain.schedule.enums.RemindType;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class ScheduleUpdateRequest {

    private String title;
    private ConditionTag conditionTag;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer estimatedTime;
    private String memo;
    private ScheduleStatus status;
    private Boolean isRemindOn;
    private Integer remindMinutes;
    private RemindType remindType;
    private RemindSoundType remindSoundType;
}
