package com.unplan.unplanserver.domain.schedule.dto.response;

import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScheduleGetResponse {

    private Long scheduleId;
    private String title;
    private String date;
    private String startTime;
    private String endTime;
    private Integer estimatedTime;
    private Boolean isQueue;
    private ScheduleStatus status;
    private ConditionTag conditionTag;

    public static ScheduleGetResponse from(Schedule schedule) {
        return ScheduleGetResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .title(schedule.getTitle())
                .date(schedule.getDate() != null ? schedule.getDate().toString() : null)
                .startTime(schedule.getStartTime() != null ? schedule.getStartTime().toString() : null)
                .endTime(schedule.getEndTime() != null ? schedule.getEndTime().toString() : null)
                .estimatedTime(schedule.getEstimatedTime())
                .isQueue(schedule.getIsQueue())
                .status(schedule.getStatus())
                .conditionTag(schedule.getConditionTag())
                .build();
    }
}
