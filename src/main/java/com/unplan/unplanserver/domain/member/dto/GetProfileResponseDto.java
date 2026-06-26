package com.unplan.unplanserver.domain.member.dto;

public record GetProfileResponseDto(
        String name,
        String nickname,
        String email
) {
}
