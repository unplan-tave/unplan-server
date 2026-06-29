package com.unplan.unplanserver.domain.measurement.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDateTime;

public class SleepRequest {

    public record SleepCreate(
            @Schema(description = "총 수면 시간(분)", example = "450")
            @NotNull(message = "수면 시간은 필수입니다.")
            @Min(value = 0, message = "수면 시간은 0분 이상이어야 합니다.")
            Integer durationMinutes,

            @Schema(description = "기상 시각", type = "string", example = "2026-06-24T07:30")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
            @NotNull(message = "기상 시각은 필수입니다.")
            @PastOrPresent(message = "기상 시각은 미래일 수 없습니다.")
            LocalDateTime wakeUpTime,

            @Schema(description = "낮잠 여부", example = "false")
            @NotNull(message = "낮잠 여부는 필수입니다.")
            Boolean isNap
    ) {
    }

    public record SleepUpdate(
            @Schema(description = "수정할 총 수면 시간(분)", example = "450")
            @NotNull(message = "수면 시간은 필수입니다.")
            @Min(value = 0, message = "수면 시간은 0분 이상이어야 합니다.")
            Integer durationMinutes,

            @Schema(description = "수정할 기상 시각", type = "string", example = "2026-06-24T07:30")
            @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
            @NotNull(message = "기상 시각은 필수입니다.")
            @PastOrPresent(message = "기상 시각은 미래일 수 없습니다.")
            LocalDateTime wakeUpTime,

            @Schema(description = "수정할 낮잠 여부", example = "false")
            @NotNull(message = "낮잠 여부는 필수입니다.")
            Boolean isNap
    ) {
    }
}