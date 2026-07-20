package com.unplan.unplanserver.domain.schedule.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ScheduleCreateResponse {

    private Long scheduleId;
    private String title;
    private String date;
    private String endDate;
    private String startTime;
    private String endTime;
    private Integer estimatedTime;
    private Boolean isQueue;
    private List<String> personalTags;
}
