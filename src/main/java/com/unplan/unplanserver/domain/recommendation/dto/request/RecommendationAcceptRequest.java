package com.unplan.unplanserver.domain.recommendation.dto.request;

/**
 * 추천 수락 요청 (POST /schedule/recommendations/{recommendId}/accept).
 *
 * @param keepQueueCard '기존 큐 카드 유지하기' 체크 여부 (Figma 컨디션 기반 추천 바텀시트).
 *                      true 면 큐 카드를 핀 카드로 복제 생성하고 원본 큐 카드는 그대로 남겨,
 *                      같은 일정이 큐(다음 추천 후보로 계속 노출)와 핀(확정)으로 공존한다.
 *                      false(기본)면 원본 큐 카드를 핀 카드로 전환한다(큐에서 사라짐).
 *                      회복 수단 추천에는 영향 없음(원본 큐 카드가 없어 항상 새 일정 생성).
 * @param recoveryMean  회복 수단(RECOVERY_MEAN) 추천을 수락할 때 사용자가 고른 회복 수단 표시명.
 *                      선택한 수단이 생성되는 일정의 제목이 된다. 큐 카드 추천에는 불필요.
 */
public record RecommendationAcceptRequest(
        Boolean keepQueueCard,
        String recoveryMean
) {
    public boolean keepQueueCardOrDefault() {
        return Boolean.TRUE.equals(keepQueueCard);
    }
}
