package com.unplan.unplanserver.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueRequestDto (
        @NotBlank String deviceId
){
}
