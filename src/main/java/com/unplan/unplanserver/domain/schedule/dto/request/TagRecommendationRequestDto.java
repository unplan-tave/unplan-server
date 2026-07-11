package com.unplan.unplanserver.domain.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TagRecommendationRequestDto(
        @NotBlank String title
) {
}
