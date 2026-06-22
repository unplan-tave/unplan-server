package com.unplan.unplanserver.domain.onboarding.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.unplan.unplanserver.domain.onboarding.enums.TransportType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OnboardingRequest(

        @Valid
        @NotNull(message = "회복 방법 정보는 필수입니다.")
        RecoverRequest.UpdateMethods recovery,

        @Valid
        @NotNull(message = "수면 컨디션 정보는 필수입니다.")
        SleepConditionRequest.UpdateConditions sleepCondition,

        @Valid
        @NotNull(message = "하루 활동 패턴 정보는 필수입니다.")
        BiorhythmRequest biorhythm,

        List<@NotNull TransportType> transportations
) {
}