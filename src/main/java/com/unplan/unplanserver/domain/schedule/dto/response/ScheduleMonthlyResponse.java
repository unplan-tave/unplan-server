package com.unplan.unplanserver.domain.schedule.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScheduleMonthlyResponse {

    private String yearMonth;
    private List<DailyCount> schedules;

    @Getter
    @Builder
    public static class DailyCount {
        private String date;
        private Integer count;
    }
}
