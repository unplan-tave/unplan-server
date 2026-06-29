package com.unplan.unplanserver.domain.measurement.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Getter;

import java.time.LocalDateTime;

public class ConditionRequest {

    @Getter
    public static class ConditionCreate {

        @NotNull(message = "신체 컨디션 점수는 필수입니다.")
        @Min(value = 0, message = "신체 컨디션 점수는 0점 이상이어야 합니다.")
        @Max(value = 6, message = "신체 컨디션 점수는 6점 이하이어야 합니다.")
        private Integer bodyScore;

        @NotNull(message = "정신 컨디션 점수는 필수입니다.")
        @Min(value = 0, message = "정신 컨디션 점수는 0점 이상이어야 합니다.")
        @Max(value = 6, message = "정신 컨디션 점수는 6점 이하이어야 합니다.")
        private Integer mindScore;

        @Schema(description = "기록 시각", type = "string", example = "2026-06-28T14:30")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        @NotNull(message = "기록 시각은 필수입니다.")
        @PastOrPresent(message = "미래 시각은 입력할 수 없습니다.")
        private LocalDateTime dateTime;
    }

    @Getter
    public static class ConditionUpdate {

        @NotNull(message = "신체 컨디션 점수는 필수입니다.")
        @Min(value = 0, message = "신체 컨디션 점수는 0점 이상이어야 합니다.")
        @Max(value = 6, message = "신체 컨디션 점수는 6점 이하이어야 합니다.")
        private Integer bodyScore;

        @NotNull(message = "정신 컨디션 점수는 필수입니다.")
        @Min(value = 0, message = "정신 컨디션 점수는 0점 이상이어야 합니다.")
        @Max(value = 6, message = "정신 컨디션 점수는 6점 이하이어야 합니다.")
        private Integer mindScore;

        @Schema(description = "수정할 기록 시각", type = "string", example = "2026-06-28T15:00")
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        @NotNull(message = "기록 시각은 필수입니다.")
        @PastOrPresent(message = "미래 시각은 입력할 수 없습니다.")
        private LocalDateTime dateTime;
    }
}