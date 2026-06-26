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
                @Index(name = "idx_conditions_created_at", columnList = "created_at")
        }
)
public class Condition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long conditionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "body_score", nullable = false)
    private Integer bodyScore;

    @Column(name = "mind_score", nullable = false)
    private Integer mindScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Condition(Member member, Integer bodyScore, Integer mindScore) {
        this.member = member;
        this.bodyScore = bodyScore;
        this.mindScore = mindScore;
        this.createdAt = LocalDateTime.now();
    }

    public void updateScores(Integer bodyScore, Integer mindScore) {
        this.bodyScore = bodyScore;
        this.mindScore = mindScore;
    }
}