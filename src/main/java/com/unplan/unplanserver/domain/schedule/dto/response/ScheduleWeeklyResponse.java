package com.unplan.unplanserver.domain.schedule.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScheduleWeeklyResponse {

    private List<DailySchedules> weeklySchedules;

    @Getter
    @Builder
    public static class DailySchedules {
        private String date;
        private List<ScheduleSummary> schedules;
    }

    @Getter
    @Builder
    public static class ScheduleSummary {
        private Long scheduleId;
        private String title;
    }
}
