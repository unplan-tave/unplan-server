package com.unplan.unplanserver.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record LogoutRequestDto(
        @NotBlank String deviceId
) {
}
