package com.unplan.unplanserver.domain.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TagRecommendationRequestDto(
        @NotBlank @Size(max = 50) String title
) {
}
