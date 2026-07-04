package com.unplan.unplanserver.domain.member.dto;

import jakarta.validation.constraints.NotNull;

public record GetProfileResponseDto(
        String name,
        String nickname,
        String email,
        @NotNull Boolean onboardingCompleted
) {
}
