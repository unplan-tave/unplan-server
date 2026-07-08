package com.unplan.unplanserver.domain.auth.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLoginResponseDto(
        @NotBlank String accessToken,
        @NotBlank String refreshToken,
        @NotNull Boolean isNewUser,
        @NotNull Boolean onboardingCompleted
) {
}
