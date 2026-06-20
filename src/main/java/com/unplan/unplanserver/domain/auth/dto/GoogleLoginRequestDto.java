package com.unplan.unplanserver.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequestDto(
        @NotBlank String googleIdToken,
        @NotBlank String deviceId) {
}
