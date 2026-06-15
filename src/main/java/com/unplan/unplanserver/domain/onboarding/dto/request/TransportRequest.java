package com.unplan.unplanserver.domain.onboarding.dto.request;

import com.unplan.unplanserver.domain.onboarding.enums.TransportType;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TransportRequest(
        @NotNull(message = "transportTypes는 필수입니다.")
        List<TransportType> transportTypes
) {
}