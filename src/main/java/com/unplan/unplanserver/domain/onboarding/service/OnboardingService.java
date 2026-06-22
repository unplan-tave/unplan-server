package com.unplan.unplanserver.domain.onboarding.service;

import com.unplan.unplanserver.domain.onboarding.dto.request.OnboardingRequest;
import com.unplan.unplanserver.domain.onboarding.dto.request.TransportRequest;
import com.unplan.unplanserver.domain.onboarding.dto.response.OnboardingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingService {

    private final RecoverService recoverService;
    private final SleepConditionService sleepConditionService;
    private final BiorhythmService biorhythmService;
    private final TransportService transportService;

    public OnboardingResponse saveOnboarding(
            Long memberId,
            OnboardingRequest request
    ) {
        recoverService.updateMethods(memberId, request.recovery());

        sleepConditionService.updateSleepCondition(memberId, request.sleepCondition());

        biorhythmService.updateBiorhythm(memberId, request.biorhythm());

        transportService.updateTransport(
                memberId,
                new TransportRequest(
                        request.transportations() == null ? List.of() : request.transportations()
                )
        );

        return OnboardingResponse.of(memberId);
    }
}