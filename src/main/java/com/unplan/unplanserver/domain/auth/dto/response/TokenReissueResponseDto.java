package com.unplan.unplanserver.domain.auth.dto.response;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueResponseDto (
        @NotBlank String accessToken,
        @NotBlank String refreshToken
){
}
