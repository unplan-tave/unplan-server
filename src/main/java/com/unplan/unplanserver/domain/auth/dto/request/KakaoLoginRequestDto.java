package com.unplan.unplanserver.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequestDto(
        @NotBlank String kakaoAccessToken,
        @NotBlank String deviceId) {
}
