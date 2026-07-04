package com.unplan.unplanserver.domain.onboarding.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "sleep_condition",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_sleep_condition_member_id",
                columnNames = "member_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SleepCondition {

    public static final int EXCESS_THRESHOLD = 720;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long memberId;

    private Integer targetDuration;

    private Integer dangerThreshold;

    private Integer lackThreshold;

    private Integer optimalThreshold;

    @Builder
    public SleepCondition(
            Long memberId,
            Integer targetDuration,
            Integer dangerThreshold,
            Integer lackThreshold,
            Integer optimalThreshold
    ) {
        this.memberId = memberId;
        this.targetDuration = targetDuration;
        this.dangerThreshold = dangerThreshold;
        this.lackThreshold = lackThreshold;
        this.optimalThreshold = optimalThreshold;
    }

    public void update(
            Integer targetDuration,
            Integer dangerThreshold,
            Integer lackThreshold,
            Integer optimalThreshold
    ) {
        this.targetDuration = targetDuration;
        this.dangerThreshold = dangerThreshold;
        this.lackThreshold = lackThreshold;
        this.optimalThreshold = optimalThreshold;
    }

    public Integer getExcessThreshold() {
        return EXCESS_THRESHOLD;
    }
}