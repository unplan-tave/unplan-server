package com.unplan.unplanserver.domain.recommendation.enums;

/**
 * 추천 일정의 처리 상태.
 * - PENDING: 추천되어 사용자에게 노출 중 (목록 조회 대상)
 * - ACCEPTED: 사용자가 수락하여 실제 Schedule 로 전환됨
 * - REJECTED: 사용자가 거절함. 재계산 시 목록에서 제외되어 "다시 안 뜸"
 */
public enum RecommendationStatus {
    PENDING,
    ACCEPTED,
    REJECTED
}
