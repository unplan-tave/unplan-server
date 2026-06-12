package com.unplan.unplanserver.domain.onboarding.dto.response;

import com.unplan.unplanserver.domain.onboarding.entity.SleepCondition;
import com.unplan.unplanserver.domain.onboarding.enums.SleepConditionType;

import java.util.List;

public class SleepConditionResponse {

    public record UpdateConditions(
            Long memberId,
            Integer targetDuration,
            List<Condition> conditions
    ) {
        public static UpdateConditions from(SleepCondition sleepCondition) {
            return new UpdateConditions(
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
    }

    public record GetConditions(
            Long memberId,
            Integer targetDuration,
            List<Condition> conditions
    ) {
        public static GetConditions from(SleepCondition sleepCondition) {
            return new GetConditions(
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
    }

    public record Condition(
            SleepConditionType type,
            Integer duration
    ) {
    }
}