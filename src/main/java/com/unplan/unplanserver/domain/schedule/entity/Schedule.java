package com.unplan.unplanserver.domain.schedule.entity;

import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleUpdateRequest;
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

    // Member 엔티티 미완성으로 임시 Long 사용. 추후 @ManyToOne으로 교체 예정
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 대표 위치 (장소명/주소, 예: "인하대학교")
    @Column(name = "location", length = 200)
    private String location;

    // 상세 위치 (대표 위치 내 세부, 예: "6호관")
    @Column(name = "location_detail", length = 200)
    private String locationDetail;

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

    public void update(ScheduleUpdateRequest request) {
        if (request.getTitle() != null) this.title = request.getTitle();
        if (request.getConditionTag() != null) this.conditionTag = request.getConditionTag();
        if (request.getDate() != null) this.date = request.getDate();
        if (request.getStartTime() != null) this.startTime = request.getStartTime();
        if (request.getEndTime() != null) this.endTime = request.getEndTime();
        if (request.getEstimatedTime() != null) this.estimatedTime = request.getEstimatedTime();
        if (request.getMemo() != null) this.memo = request.getMemo();
        if (request.getLocation() != null) this.location = request.getLocation();
        if (request.getLocationDetail() != null) this.locationDetail = request.getLocationDetail();
        if (request.getStatus() != null) this.status = request.getStatus();
        if (request.getIsRemindOn() != null) this.isRemindOn = request.getIsRemindOn();
        if (request.getRemindMinutes() != null) this.remindMinutes = request.getRemindMinutes();
        if (request.getRemindType() != null) this.remindType = request.getRemindType();
        if (request.getRemindSoundType() != null) this.remindSoundType = request.getRemindSoundType();
        this.isQueue = (this.startTime == null && this.endTime == null);
    }

    /**
     * 추천 수락: 큐 카드에 날짜·시간을 부여해 핀 카드로 전환한다.
     * 제목·태그·위치·메모는 큐 카드에 입력된 값 그대로 유지한다 (Figma "핀카드로 전환하기").
     */
    public void assignToPin(LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isQueue = (startTime == null && endTime == null);
    }

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