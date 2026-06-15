package com.unplan.unplanserver.domain.onboarding.dto.response;

import com.unplan.unplanserver.domain.onboarding.enums.TransportType;

import java.util.List;

public record TransportResponse(
        Long memberId,
        List<TransportType> transportTypes
) {
}