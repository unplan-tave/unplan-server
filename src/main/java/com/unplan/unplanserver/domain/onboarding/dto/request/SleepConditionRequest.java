package com.unplan.unplanserver.domain.onboarding.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;

public class SleepConditionRequest {

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record UpdateConditions(
            @NotNull Integer targetDuration,
            @NotNull Integer dangerThreshold,
            @NotNull Integer lackThreshold,
            @NotNull Integer optimalThreshold
    ) {
    }
}