package com.unplan.unplanserver.domain.measurement.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

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
    }
}