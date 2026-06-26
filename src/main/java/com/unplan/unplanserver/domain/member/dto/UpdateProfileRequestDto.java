package com.unplan.unplanserver.domain.member.dto;

import jakarta.validation.constraints.Email;

public record UpdateProfileRequestDto(
        String name,
        String nickname,
        @Email String email
) {
}
