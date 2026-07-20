package com.unplan.unplanserver.domain.schedule.dto.response;

import com.unplan.unplanserver.domain.schedule.entity.LocationInfo;
import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.domain.schedule.enums.RemindSoundType;
import com.unplan.unplanserver.domain.schedule.enums.RemindType;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ScheduleDetailResponse {

    private Long scheduleId;
    private String title;
    private String date;
    private String endDate;
    private String startTime;
    private String endTime;
    private Integer estimatedTime;
    private Boolean isQueue;
    private ScheduleStatus status;
    private ConditionTag conditionTag;
    private String memo;
    private String location;         // 대표 위치 (예: 인하대학교)
    private String locationDetail;   // 상세 위치 (예: 6호관)
    private Boolean isRemindOn;
    private Integer remindMinutes;
    private RemindType remindType;
    private RemindSoundType remindSoundType;
    private Boolean isRecurring;
    private Boolean isConflict;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<String> personalTags;
    // 반복 규칙 상세. 반복 일정이 아니면(is_recurring=false) null. 생성 요청의 recurrence 와 동일한 구조.
    private Recurrence recurrence;

    /** 반복 규칙 상세 (생성 요청 recurrence 와 동일 필드) */
    @Getter
    @Builder
    public static class Recurrence {
        private RecurrenceFreq freq;      // 반복 빈도 DAILY/WEEKLY/MONTHLY/YEARLY
        private Integer interval;         // 반복 간격 'N마다'
        private String byDay;             // 반복 요일 "MON,WED" (WEEKLY/MONTHLY)
        private String byMonthDay;        // 매월 반복 일자 "1,17" (MONTHLY)
        private LocalDate until;          // 종료일(포함), 없으면 null
        private Integer count;            // 총 발생 횟수(원본 포함), 없으면 null

        private static Recurrence from(RecurrenceRule rule) {
            return Recurrence.builder()
                    .freq(rule.getFreq())
                    .interval(rule.getInterval())
                    .byDay(rule.getByDay())
                    .byMonthDay(rule.getByMonthDay())
                    .until(rule.getUntil())
                    .count(rule.getCount())
                    .build();
        }
    }

    public static ScheduleDetailResponse from(Schedule schedule, LocationInfo locationInfo,
                                              List<String> personalTags, RecurrenceRule recurrenceRule) {
        return ScheduleDetailResponse.builder()
                .scheduleId(schedule.getScheduleId())
                .title(schedule.getTitle())
                .date(schedule.getDate() != null ? schedule.getDate().toString() : null)
                .endDate(schedule.getEndDate() != null ? schedule.getEndDate().toString() : null)
                .startTime(schedule.getStartTime() != null ? schedule.getStartTime().toString() : null)
                .endTime(schedule.getEndTime() != null ? schedule.getEndTime().toString() : null)
                .estimatedTime(schedule.getEstimatedTime())
                .isQueue(schedule.getIsQueue())
                .status(schedule.getStatus())
                .conditionTag(schedule.getConditionTag())
                .memo(schedule.getMemo())
                .location(schedule.getLocation())
                .locationDetail(schedule.getLocationDetail())
                .isRemindOn(schedule.getIsRemindOn())
                .remindMinutes(schedule.getRemindMinutes())
                .remindType(schedule.getRemindType())
                .remindSoundType(schedule.getRemindSoundType())
                .isRecurring(schedule.getIsRecurring())
                .isConflict(schedule.getIsConflict())
                .latitude(locationInfo != null ? locationInfo.getLatitude() : null)
                .longitude(locationInfo != null ? locationInfo.getLongitude() : null)
                .personalTags(personalTags)
                .recurrence(recurrenceRule != null ? Recurrence.from(recurrenceRule) : null)
                .build();
    }
}
