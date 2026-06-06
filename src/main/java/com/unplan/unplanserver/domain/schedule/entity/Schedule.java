package com.unplan.unplanserver.domain.schedule.entity;

import com.unplan.unplanserver.domain.schedule.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    // Member 엔티티가 아직 없어 임시로 Long 사용. 추후 @ManyToOne으로 교체 예정
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "location", length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ScheduleStatus status;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "is_queue")
    private Boolean isQueue;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "estimated_time")
    private Integer estimatedTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_tag")
    private ConditionTag conditionTag;

    @Column(name = "remind_minutes")
    private Integer remindMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "remind_type")
    private RemindType remindType;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "is_recurring")
    private Boolean isRecurring;

    @Column(name = "original_date")
    private LocalDate originalDate;

    @Column(name = "is_remind_on")
    private Boolean isRemindOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "remind_sound_type")
    private RemindSoundType remindSoundType;

    @Column(name = "is_conflict")
    private Boolean isConflict;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "update_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}