package com.unplan.unplanserver.domain.onboarding.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "biorhythm",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_biorhythm_member_id",
                columnNames = "member_id"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Biorhythm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 24)
    private String focusedTimeline;

    @Column(nullable = false, length = 24)
    private String drowsyTimeline;

    @Column(nullable = false, length = 24)
    private String sleepTimeline;

    @Builder
    public Biorhythm(
            Long memberId,
            String focusedTimeline,
            String drowsyTimeline,
            String sleepTimeline
    ) {
        this.memberId = memberId;
        this.focusedTimeline = focusedTimeline;
        this.drowsyTimeline = drowsyTimeline;
        this.sleepTimeline = sleepTimeline;
    }

    public void update(
            String focusedTimeline,
            String drowsyTimeline,
            String sleepTimeline
    ) {
        this.focusedTimeline = focusedTimeline;
        this.drowsyTimeline = drowsyTimeline;
        this.sleepTimeline = sleepTimeline;
    }
}