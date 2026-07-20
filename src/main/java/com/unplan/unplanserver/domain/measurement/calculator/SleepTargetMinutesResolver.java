package com.unplan.unplanserver.domain.measurement.calculator;

import com.unplan.unplanserver.domain.onboarding.dto.response.SleepConditionResponse;
import com.unplan.unplanserver.domain.onboarding.service.SleepConditionService;
import com.unplan.unplanserver.domain.onboarding.enums.SleepConditionType;
import com.unplan.unplanserver.domain.measurement.calculator.MeasurementCommentCalculator.SleepConditionSettings;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;

public final class SleepTargetMinutesResolver {

    public static final int DEFAULT_TARGET_SLEEP_MINUTES = 480;
    private static final int DEFAULT_DANGER_THRESHOLD = 360;
    private static final int DEFAULT_LACK_THRESHOLD = 420;
    private static final int DEFAULT_OPTIMAL_THRESHOLD = 600;

    private SleepTargetMinutesResolver() {
    }

    public static int resolve(SleepConditionService sleepConditionService, Long memberId) {
        return resolveSettings(sleepConditionService, memberId).targetSleepMinutes();
    }

    public static SleepConditionSettings resolveSettings(
            SleepConditionService sleepConditionService,
            Long memberId
    ) {
        try {
            SleepConditionResponse sleepCondition = sleepConditionService.getSleepCondition(memberId);
            if (sleepCondition != null && sleepCondition.targetDuration() != null) {
                return new SleepConditionSettings(
                        sleepCondition.targetDuration(),
                        threshold(sleepCondition, SleepConditionType.DANGER, DEFAULT_DANGER_THRESHOLD),
                        threshold(sleepCondition, SleepConditionType.LACK, DEFAULT_LACK_THRESHOLD),
                        threshold(sleepCondition, SleepConditionType.OPTIMAL, DEFAULT_OPTIMAL_THRESHOLD)
                );
            }
        } catch (CustomException ignored) {
            if (ignored.getErrorCode() == ErrorCode.SLEEP_CONDITION_NOT_FOUND) {

            }
        }

        return defaults();
    }

    public static SleepConditionSettings defaults() {
        return new SleepConditionSettings(
                DEFAULT_TARGET_SLEEP_MINUTES,
                DEFAULT_DANGER_THRESHOLD,
                DEFAULT_LACK_THRESHOLD,
                DEFAULT_OPTIMAL_THRESHOLD
        );
    }

    private static int threshold(
            SleepConditionResponse response,
            SleepConditionType type,
            int fallback
    ) {
        if (response.conditions() == null) return fallback;
        return response.conditions().stream()
                .filter(condition -> condition.type() == type)
                .map(SleepConditionResponse.Condition::duration)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(fallback);
    }
}
