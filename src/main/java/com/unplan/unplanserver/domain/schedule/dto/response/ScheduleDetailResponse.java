package com.unplan.unplanserver.domain.schedule.dto.response;

import com.unplan.unplanserver.domain.schedule.entity.LocationInfo;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RemindSoundType;
import com.unplan.unplanserver.domain.schedule.enums.RemindType;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ScheduleDetailResponse {

    private Long scheduleId;
    private String title;
    private String date;
    private String startTime;
    private String endTime;
    private Integer estimatedTime;
    private Boolean isQueue;
    private ScheduleStatus status;
    private ConditionTag conditionTag;
    private String memo;
    private String location;
    private Boolean isRemindOn;
    private Integer remindMinutes;
    private RemindType remindType;
    private RemindSoundType remindSoundType;
    private Boolean isRecurring;
    private Boolean isConflict;
    private BigDecimal latitude;
    private BigDecimal longitude;

    public static ScheduleDetailResponse from(Schedule schedule, LocationInfo locationInfo) {
        return ScheduleDetailResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .title(schedule.getTitle())
                .date(schedule.getDate() != null ? schedule.getDate().toString() : null)
                .startTime(schedule.getStartTime() != null ? schedule.getStartTime().toString() : null)
                .endTime(schedule.getEndTime() != null ? schedule.getEndTime().toString() : null)
                .estimatedTime(schedule.getEstimatedTime())
                .isQueue(schedule.getIsQueue())
                .status(schedule.getStatus())
                .conditionTag(schedule.getConditionTag())
                .memo(schedule.getMemo())
                .location(schedule.getLocation())
                .isRemindOn(schedule.getIsRemindOn())
                .remindMinutes(schedule.getRemindMinutes())
                .remindType(schedule.getRemindType())
                .remindSoundType(schedule.getRemindSoundType())
                .isRecurring(schedule.getIsRecurring())
                .isConflict(schedule.getIsConflict())
                .latitude(locationInfo != null ? locationInfo.getLatitude() : null)
                .longitude(locationInfo != null ? locationInfo.getLongitude() : null)
                .build();
    }
}
