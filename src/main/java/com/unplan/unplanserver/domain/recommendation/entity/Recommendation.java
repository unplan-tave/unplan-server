package com.unplan.unplanserver.domain.recommendation.entity;

import com.unplan.unplanserver.domain.recommendation.enums.RecommendationSourceType;
import com.unplan.unplanserver.domain.recommendation.enums.RecommendationStatus;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 사용자에게 노출된 추천 일정 한 건. (Notion "추천 로직" / 추천 API 명세)
 * GET 목록 조회 시 추천 엔진 결과를 행으로 영속화하고, 그 PK(recommendId)로 수락/거절을 처리한다.
 * title/startTime/endTime/conditionTag 는 추천 시점 스냅샷 — 이후 원본 큐카드가 바뀌거나 삭제돼도 추천은 보존된다.
 */
@Entity
@Table(name = "recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommend_id")
    private Long recommendId;

    // Member 엔티티 미완성으로 임시 Long 사용. 추후 @ManyToOne으로 교체 예정 (Schedule 엔티티와 동일 컨벤션)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 추천 대상 날짜. 홈/컨디션 추천은 오늘, 큐카드 7일 추천은 D+1~D+7
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "title", length = 100)
    private String title;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_tag")
    private ConditionTag conditionTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private RecommendationSourceType sourceType;

    // 큐카드 기반이면 원본 큐카드(Schedule) id, 회복 수단 기반이면 null
    @Column(name = "source_schedule_id")
    private Long sourceScheduleId;

    // 추천 목록 내 노출 순서 (0부터). 바텀시트가 "추천 일정 1", "1/4"처럼 순서대로 페이지네이션되므로
    // (Figma UI & Prototype), 엔진 정렬 결과를 저장해 재조회 시에도 같은 순서를 보장한다.
    @Column(name = "display_order")
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RecommendationStatus status;

    // 수락 시 생성된 실제 일정(Schedule) id (추적용). 수락 전에는 null
    @Column(name = "accepted_schedule_id")
    private Long acceptedScheduleId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 수락 처리: 상태를 ACCEPTED로 바꾸고 생성된 일정 id를 기록한다. */
    public void accept(Long acceptedScheduleId) {
        this.status = RecommendationStatus.ACCEPTED;
        this.acceptedScheduleId = acceptedScheduleId;
    }

    /** 거절 처리: 재계산 시 제외되도록 REJECTED로 표시한다. */
    public void reject() {
        this.status = RecommendationStatus.REJECTED;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = RecommendationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
