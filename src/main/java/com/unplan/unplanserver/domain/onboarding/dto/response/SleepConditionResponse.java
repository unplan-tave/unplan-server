package com.unplan.unplanserver.domain.onboarding.dto.response;

import com.unplan.unplanserver.domain.onboarding.entity.SleepCondition;
import com.unplan.unplanserver.domain.onboarding.enums.SleepConditionType;

import java.util.List;

public record SleepConditionResponse(
        Long memberId,
        Integer targetDuration,
        List<Condition> conditions
) {
    public static SleepConditionResponse from(SleepCondition sleepCondition) {
        return new SleepConditionResponse(
                sleepCondition.getMemberId(),
                sleepCondition.getTargetDuration(),
                List.of(
                        new Condition(SleepConditionType.DANGER, sleepCondition.getDangerThreshold()),
                        new Condition(SleepConditionType.LACK, sleepCondition.getLackThreshold()),
                        new Condition(SleepConditionType.OPTIMAL, sleepCondition.getOptimalThreshold()),
                        new Condition(SleepConditionType.EXCESS, sleepCondition.getExcessThreshold())
                )
        );
    }

    public record Condition(
            SleepConditionType type,
            Integer duration
    ) {
    }
}