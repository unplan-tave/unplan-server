package com.unplan.unplanserver.domain.member.dto;

public record ProfileResponseDto(
        String name,
        String nickname,
        String email
) {
}
