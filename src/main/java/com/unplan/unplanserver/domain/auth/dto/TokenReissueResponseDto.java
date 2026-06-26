package com.unplan.unplanserver.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueResponseDto (
        @NotBlank String accessToken,
        @NotBlank String refreshToken
){
}
