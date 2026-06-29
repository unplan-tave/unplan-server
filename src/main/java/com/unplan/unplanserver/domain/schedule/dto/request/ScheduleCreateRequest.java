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
        @Schema(
            description = "반복 요일. WEEKLY: 'MON,TUE,FRI' 형식 (복수 가능). MONTHLY 같은 요일: '2WED'(N번째 요일) 또는 'TUE'(원본 날짜 기준 자동 계산)",
            example = "MON,TUE"
        )
        private String byDay;
        @Schema(description = "MONTHLY 같은 날짜 반복. 단일 또는 복수 날짜 '16' 또는 '1,17'", example = "16")
        private String byMonthDay;
        @Schema(example = "2026-12-31", description = "종료일 (종료 안 함이면 null)")
        private LocalDate until;

        private Integer count;
    }
}