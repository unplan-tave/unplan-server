package com.unplan.unplanserver.domain.onboarding.dto.request;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BiorhythmRequest(

        @NotBlank
        @Size(min = 24, max = 24)
        @Pattern(regexp = "^[01]{24}$")
        String focusedTimeline,

        @NotBlank
        @Size(min = 24, max = 24)
        @Pattern(regexp = "^[01]{24}$")
        String drowsyTimeline,

        @NotBlank
        @Size(min = 24, max = 24)
        @Pattern(regexp = "^[01]{24}$")
        String sleepTimeline
) {
}