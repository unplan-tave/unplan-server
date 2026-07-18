package com.unplan.unplanserver.domain.measurement.entity;

import com.unplan.unplanserver.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "conditions",
        indexes = {
                @Index(name = "idx_conditions_member_id", columnList = "member_id"),
                @Index(name = "idx_conditions_measured_at", columnList = "measured_at")
        }
)
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "condition_id")
    private Long conditionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "body_score", nullable = false)
    private Integer bodyScore;

    @Column(name = "mind_score", nullable = false)
    private Integer mindScore;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;

    public Condition(Member member, Integer bodyScore, Integer mindScore, LocalDateTime measuredAt) {
        this.member = member;
        this.bodyScore = bodyScore;
        this.mindScore = mindScore;
        this.measuredAt = measuredAt;
    }

    public void updateScoresAndDateTime(Integer bodyScore, Integer mindScore, LocalDateTime measuredAt) {
        this.bodyScore = bodyScore;
        this.mindScore = mindScore;
        this.measuredAt = measuredAt;
    }
}
