package com.unplan.unplanserver.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TokenReissueRequestDto (
        @NotBlank String deviceId
){
}
