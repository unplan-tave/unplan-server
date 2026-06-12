package com.unplan.unplanserver.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SocialLoginResponseDto(
        @NotBlank String accessToken,
        @NotBlank String refreshToken,
        @NotBlank Boolean isNewUser
) {
}
