package com.unplan.unplanserver.domain.onboarding.service;

import com.unplan.unplanserver.domain.onboarding.dto.request.BiorhythmRequest;
import com.unplan.unplanserver.domain.onboarding.dto.request.OnboardingRequest;
import com.unplan.unplanserver.domain.onboarding.dto.request.RecoverRequest;
import com.unplan.unplanserver.domain.onboarding.dto.request.SleepConditionRequest;
import com.unplan.unplanserver.domain.onboarding.dto.request.TransportRequest;
import com.unplan.unplanserver.domain.onboarding.dto.response.OnboardingResponse;
import com.unplan.unplanserver.domain.onboarding.enums.TransportType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OnboardingService {

    private static final String EMPTY_TIMELINE = "000000000000000000000000";

    private final RecoverService recoverService;
    private final SleepConditionService sleepConditionService;
    private final BiorhythmService biorhythmService;
    private final TransportService transportService;

    public OnboardingResponse saveOnboarding(
            Long memberId,
            OnboardingRequest request
    ) {
        recoverService.updateMethods(
                memberId,
                new RecoverRequest.UpdateMethods(
                        request.recovery().defaultMethods(),
                        request.recovery().customMethods()
                )
        );

        sleepConditionService.updateSleepCondition(
                memberId,
                new SleepConditionRequest.UpdateConditions(
                        request.sleepCondition().targetDuration(),
                        request.sleepCondition().dangerThreshold(),
                        request.sleepCondition().lackThreshold(),
                        request.sleepCondition().optimalThreshold()
                )
        );

        biorhythmService.updateBiorhythm(
                memberId,
                new BiorhythmRequest(
                        defaultTimeline(request.biorhythm().focusedTimeline()),
                        defaultTimeline(request.biorhythm().drowsyTimeline()),
                        request.biorhythm().sleepTimeline()
                )
        );

        transportService.updateTransport(
                memberId,
                new TransportRequest(defaultTransportations(request.transportations()))
        );

        return OnboardingResponse.of(memberId);
    }

    private String defaultTimeline(String timeline) {
        return timeline == null ? EMPTY_TIMELINE : timeline;
    }

    private List<TransportType> defaultTransportations(List<TransportType> transportations) {
        return transportations == null ? List.of() : transportations;
    }
}