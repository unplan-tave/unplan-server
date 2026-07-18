package com.unplan.unplanserver.domain.measurement.calculator;

import com.unplan.unplanserver.domain.onboarding.dto.response.SleepConditionResponse;
import com.unplan.unplanserver.domain.onboarding.service.SleepConditionService;
import com.unplan.unplanserver.global.exception.CustomException;

public final class SleepTargetMinutesResolver {

    public static final int DEFAULT_TARGET_SLEEP_MINUTES = 480;

    private SleepTargetMinutesResolver() {
    }

    public static int resolve(SleepConditionService sleepConditionService, Long memberId) {
        try {
            SleepConditionResponse sleepCondition = sleepConditionService.getSleepCondition(memberId);
            if (sleepCondition != null && sleepCondition.targetDuration() != null) {
                return sleepCondition.targetDuration();
            }
        } catch (CustomException ignored) {
        }

        return DEFAULT_TARGET_SLEEP_MINUTES;
    }
}
