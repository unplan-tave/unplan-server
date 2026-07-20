package com.unplan.unplanserver.domain.schedule.dto.request;

import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RemindSoundType;
import com.unplan.unplanserver.domain.schedule.enums.RemindType;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class ScheduleUpdateRequest {

    private String title;
    private ConditionTag conditionTag;

    @Schema(description = "개인 태그 이름 목록 (일정당 최대 10개, 각 1~25자). 전달하면 일정의 태그 전체가 이 목록으로 교체됨 — 미전송(null)이면 기존 유지, 빈 배열이면 전체 해제", example = "[\"자기계발\", \"건강\"]")
    @Size(max = 10, message = "개인 태그는 최대 10개까지 등록할 수 있습니다")
    private List<@Size(max = 25, message = "태그 이름은 25자를 초과할 수 없습니다") String> personalTags;
    @Schema(example = "2026-06-20")
    private LocalDate date;

    @Schema(description = "기간 일정 종료 날짜(포함). 명시적 null이면 단일 날짜로 변경하고, 필드 미전송이면 기존 값 유지", example = "2026-06-22")
    private LocalDate endDate;
    private boolean endDatePresent;

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.endDatePresent = true;
    }

    public boolean isEndDatePresent() {
        return endDatePresent;
    }

    @Schema(example = "09:00")
    private LocalTime startTime;

    @Schema(example = "10:00")
    private LocalTime endTime;

    // 0/음수 소요시간은 추천 소요시간 필터를 무의미하게 통과하므로 차단
    @Positive
    private Integer estimatedTime;

    private String memo;

    @Schema(description = "대표 위치(장소명/주소 등, 최대 200자). 전달하면 갱신, 미전송(null)이면 기존 유지", example = "인하대학교")
    @Size(max = 200, message = "위치는 200자를 초과할 수 없습니다")
    private String location;

    @Schema(description = "상세 위치(대표 위치 내 세부, 최대 200자). 전달하면 갱신, 미전송(null)이면 기존 유지", example = "6호관")
    @Size(max = 200, message = "상세 위치는 200자를 초과할 수 없습니다")
    private String locationDetail;

    private ScheduleStatus status;
    private Boolean isRemindOn;

    @PositiveOrZero
    private Integer remindMinutes;

    private RemindType remindType;
    private RemindSoundType remindSoundType;
}
