package com.unplan.unplanserver.domain.schedule.dto.response;

import jakarta.validation.constraints.NotBlank;

public record TagRecommendationResponseDto(
        @NotBlank String recommendedTag
) {
}
