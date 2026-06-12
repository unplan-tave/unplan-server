package com.unplan.unplanserver.domain.onboarding.dto.request;

import jakarta.validation.constraints.NotNull;

public class SleepConditionRequest {

    public record UpdateConditions(
            @NotNull Integer targetDuration,
            @NotNull Integer dangerThreshold,
            @NotNull Integer lackThreshold,
            @NotNull Integer optimalThreshold
    ) {
    }
}