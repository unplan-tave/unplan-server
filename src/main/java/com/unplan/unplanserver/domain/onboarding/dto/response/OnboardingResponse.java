package com.unplan.unplanserver.domain.onboarding.dto.response;

public record OnboardingResponse(
        Long memberId
) {

    public static OnboardingResponse of(Long memberId) {
        return new OnboardingResponse(memberId);
    }
}