package com.unplan.unplanserver.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "schedule_personal_tag",
        // 한 일정에 같은 태그가 두 번 연결되지 않도록 방어
        uniqueConstraints = @UniqueConstraint(
                name = "uk_schedule_personal_tag",
                columnNames = {"schedule_id", "personal_tag_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SchedulePersonalTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_personal_tag_id")
    private Long schedulePersonalTagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "personal_tag_id", nullable = false)
    private PersonalTag personalTag;
}
