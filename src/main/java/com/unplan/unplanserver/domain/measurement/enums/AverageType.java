package com.unplan.unplanserver.domain.measurement.enums;

public enum AverageType {
    ALL,
    CONDITION,
    SLEEP;

    public boolean includesCondition() {
        return this == ALL || this == CONDITION;
    }

    public boolean includesSleep() {
        return this == ALL || this == SLEEP;
    }
}
