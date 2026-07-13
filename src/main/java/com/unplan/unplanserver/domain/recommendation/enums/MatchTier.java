package com.unplan.unplanserver.domain.recommendation.enums;

/**
 * 추천 카드의 매칭 순위 (Notion "추천 로직" 3단계 티어).
 * 컨디션 탭 멘트의 적합도 문장이 [현재 상태 × 매칭 순위] 표로 고정 문구를 고르는데,
 * 순위는 서버 매칭 엔진만 아는 정보라 응답에 함께 내려준다.
 * 기력 회복 상태의 회복 태그 카드는 정확 일치(EXACT)로 취급하고,
 * '회복 수단' 후보는 태그 매칭 결과가 아니므로 순위 없음(null) — sourceType(RECOVERY_MEAN)으로 구분한다.
 */
public enum MatchTier {
    /** 1순위: 현재 컨디션 태그와 정확 일치 */
    EXACT,
    /** 2순위: 인접 태그 (RecommendationMatcher.ADJACENT 표) */
    ADJACENT,
    /** 3순위: 태그 무관 나머지 — 마감 임박 폴백 */
    DEADLINE
}
