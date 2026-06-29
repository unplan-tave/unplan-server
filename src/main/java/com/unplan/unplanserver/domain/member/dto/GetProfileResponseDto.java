package com.unplan.unplanserver.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

public record GetProfileResponseDto(
        String name,
        String nickname,
        String email,
        @NotBlank Boolean onboardingCompleted
) {
}
