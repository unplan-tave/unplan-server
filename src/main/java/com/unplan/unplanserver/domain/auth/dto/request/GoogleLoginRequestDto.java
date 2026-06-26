package com.unplan.unplanserver.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequestDto(
        @NotBlank String googleIdToken,
        @NotBlank String deviceId) {
}
