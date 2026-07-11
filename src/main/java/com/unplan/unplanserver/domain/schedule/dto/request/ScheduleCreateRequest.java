package com.unplan.unplanserver.domain.schedule.dto.request;

import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.domain.schedule.enums.RemindSoundType;
import com.unplan.unplanserver.domain.schedule.enums.RemindType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Schema(description = "개인 태그 이름 목록 (선택, 일정당 최대 10개, 각 1~25자). 같은 이름은 대소문자 무시하고 재사용됨", example = "[\"자기계발\", \"건강\"]")
    @Size(max = 10, message = "개인 태그는 최대 10개까지 등록할 수 있습니다")
    private List<@Size(max = 25, message = "태그 이름은 25자를 초과할 수 없습니다") String> personalTags;

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

    @Schema(description = """
        반복 설정. 생략 시 반복 없는 단일 일정.
        - DAILY/YEARLY: freq, interval 만 사용 (by_day·by_month_day 무시)
        - WEEKLY: freq, interval (+선택 by_day)
        - MONTHLY: freq, interval + by_day '또는' by_month_day 중 하나 (둘 다 주면 by_day 우선, 둘 다 생략 시 원본 일자 기준)
        - until/count 는 모든 빈도에서 선택""")
    @Getter
    @NoArgsConstructor
    public static class RecurrenceRequest {

        @Schema(description = "반복 빈도 (필수)", example = "MONTHLY")
        @NotNull
        private RecurrenceFreq freq;

        @Schema(description = "반복 간격 'N마다' (필수, 1 이상). 1=매번, 2=한 칸 건너뛰기", example = "1")
        @NotNull
        private Integer interval;

        @Schema(description = """
            [WEEKLY] 반복 요일, 복수 가능: 'MON,WED,FRI' (생략 시 원본 날짜의 요일).
            [MONTHLY] 'N번째 요일'을 출현 횟수로 지정: '2WED'(2번째 수요일) / 'MON,WED'(복수 가능).
            숫자 없이 'TUE'만 주면 원본 날짜 기준으로 N번째를 자동 계산.
            N번째가 없는 달은 마지막 출현 요일로 폴백.
            토큰: SUN/MON/TUE/WED/THU/FRI/SAT. [DAILY/YEARLY 에서는 무시]""",
            example = "MON,WED")
        private String byDay;

        @Schema(description = """
            [MONTHLY 전용] 매월 반복할 일자, 복수 가능: '16' 또는 '1,17'.
            해당 일자가 없는 달(예: 2월 31일)은 그 달 말일로 당김. 1~31 범위 밖 값은 무시.
            (by_day 가 함께 오면 무시됨)""",
            example = "16")
        private String byMonthDay;

        @Schema(description = "종료일 (inclusive, 그날 포함). 종료 안 함이면 null", example = "2026-12-31")
        private LocalDate until;

        @Schema(description = "총 발생 횟수 (원본 포함). 예: 3 → 원본 + 2회. 무제한이면 null. until 과 함께 주면 더 빨리 끝나는 쪽 적용", example = "10")
        private Integer count;
    }
}