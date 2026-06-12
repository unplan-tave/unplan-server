package com.unplan.unplanserver.domain.onboarding.service;

import com.unplan.unplanserver.domain.onboarding.dto.request.SleepConditionRequest;
import com.unplan.unplanserver.domain.onboarding.dto.response.SleepConditionResponse;
import com.unplan.unplanserver.domain.onboarding.entity.SleepCondition;
import com.unplan.unplanserver.domain.onboarding.repository.SleepConditionRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SleepConditionService {

    private static final int EXCESS_THRESHOLD = 720;
    private static final int TIME_UNIT = 30;
    private static final int MIN_SECTION_DURATION = 30;
    private static final int MAX_SECTION_DURATION = 630;

    private final SleepConditionRepository sleepConditionRepository;

    @Transactional
    public SleepConditionResponse.UpdateConditions updateSleepCondition(
            Long memberId,
            SleepConditionRequest.UpdateConditions request
    ) {
        validateSleepUnit(request);
        validateSleepOrder(request);
        validateSectionRange(request);
        validateTargetDuration(request.targetDuration());

        SleepCondition sleepCondition = sleepConditionRepository.findByMemberId(memberId)
                .map(existing -> {
                    existing.update(
                            request.targetDuration(),
                            request.dangerThreshold(),
                            request.lackThreshold(),
                            request.optimalThreshold()
                    );
                    return existing;
                })
                .orElseGet(() -> sleepConditionRepository.save(
                        SleepCondition.builder()
                                .memberId(memberId)
                                .targetDuration(request.targetDuration())
                                .dangerThreshold(request.dangerThreshold())
                                .lackThreshold(request.lackThreshold())
                                .optimalThreshold(request.optimalThreshold())
                                .build()
                ));

        return SleepConditionResponse.UpdateConditions.from(sleepCondition);
    }

    public SleepConditionResponse.GetConditions getSleepCondition(Long memberId) {
        SleepCondition sleepCondition = sleepConditionRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST));

        return SleepConditionResponse.GetConditions.from(sleepCondition);
    }

    private void validateSleepUnit(SleepConditionRequest.UpdateConditions request) {
        if (request.targetDuration() % TIME_UNIT != 0
                || request.dangerThreshold() % TIME_UNIT != 0
                || request.lackThreshold() % TIME_UNIT != 0
                || request.optimalThreshold() % TIME_UNIT != 0) {
            throw new CustomException(ErrorCode.INVALID_SLEEP_UNIT);
        }
    }

    private void validateSleepOrder(SleepConditionRequest.UpdateConditions request) {
        boolean isValidOrder =
                request.dangerThreshold() < request.lackThreshold()
                        && request.lackThreshold() < request.optimalThreshold()
                        && request.optimalThreshold() < EXCESS_THRESHOLD;

        if (!isValidOrder) {
            throw new CustomException(ErrorCode.INVALID_SLEEP_ORDER);
        }
    }

    private void validateSectionRange(SleepConditionRequest.UpdateConditions request) {
        int dangerSection = request.dangerThreshold();
        int lackSection = request.lackThreshold() - request.dangerThreshold();
        int optimalSection = request.optimalThreshold() - request.lackThreshold();
        int excessSection = EXCESS_THRESHOLD - request.optimalThreshold();

        if (dangerSection < MIN_SECTION_DURATION
                || lackSection < MIN_SECTION_DURATION
                || optimalSection < MIN_SECTION_DURATION
                || excessSection < MIN_SECTION_DURATION) {
            throw new CustomException(ErrorCode.INVALID_SLEEP_RANGE);
        }

        if (dangerSection > MAX_SECTION_DURATION
                || lackSection > MAX_SECTION_DURATION
                || optimalSection > MAX_SECTION_DURATION
                || excessSection > MAX_SECTION_DURATION) {
            throw new CustomException(ErrorCode.INVALID_SLEEP_RANGE);
        }
    }

    private void validateTargetDuration(Integer targetDuration) {
        if (targetDuration <= 0 || targetDuration > EXCESS_THRESHOLD) {
            throw new CustomException(ErrorCode.INVALID_SLEEP_RANGE);
        }
    }
}