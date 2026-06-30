package com.unplan.unplanserver.domain.recommendation.enums;

/**
 * 추천이 무엇으로부터 도출됐는지 구분.
 * - QUEUE_CARD: 큐 카드(Schedule isQueue=true)에서 도출. source_schedule_id 보유
 * - RECOVERY_MEAN: '기력 회복' 상태에서 온보딩/설정의 회복 수단으로 도출. source_schedule_id 없음
 */
public enum RecommendationSourceType {
    QUEUE_CARD,
    RECOVERY_MEAN
}
