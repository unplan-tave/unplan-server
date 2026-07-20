package com.unplan.unplanserver.domain.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 추천 '패스' 기록 한 건. 사용자가 특정 날짜의 추천 카드를 패스하면 그 원본 큐 카드를
 * 해당 날짜의 추천에서 제외한다(삭제가 아니라 '그날만 안 뜸' — PM 확정).
 *
 * 추천 행(recommendation)은 GET 조회마다 미수락분이 지워지고 재생성되므로, 패스 상태는
 * 이 별도 테이블에 (member_id, date, source_schedule_id) 로 남겨 재생성에도 살아남게 한다.
 * date 를 키에 포함하므로 다음 날 같은 큐 카드는 다시 추천 후보가 된다.
 */
@Entity
@Table(name = "recommendation_pass",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_recommendation_pass",
                columnNames = {"member_id", "date", "source_schedule_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecommendationPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_pass_id")
    private Long recommendationPassId;

    // Member 엔티티 미완성으로 임시 Long 사용 (Recommendation 과 동일 컨벤션)
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 패스가 적용되는 추천 대상 날짜 (홈/컨디션 추천 = 오늘)
    @Column(name = "date", nullable = false)
    private LocalDate date;

    // 패스한 추천의 원본 큐 카드(Schedule) id. 회복 수단 추천은 패스 대상이 아니므로 항상 값이 있다.
    @Column(name = "source_schedule_id", nullable = false)
    private Long sourceScheduleId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
