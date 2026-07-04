package com.unplan.unplanserver.domain.auth.dto.response;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginResponseDto(
        @NotBlank String accessToken,
        @NotBlank String refreshToken,
        @NotBlank Boolean isNewUser,
        @NotBlank Boolean onboardingCompleted
) {
}
