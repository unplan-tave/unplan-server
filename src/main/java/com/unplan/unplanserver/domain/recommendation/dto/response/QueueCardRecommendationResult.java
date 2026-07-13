package com.unplan.unplanserver.domain.recommendation.dto.response;

/**
 * 큐 카드 7일 추천 생성 결과 (서비스 → 컨트롤러 전달용).
 * 성공(슬롯 있음)이면 {@code success}, 무슬롯이면 {@code noSlot} 중 하나만 채워진다.
 * 컨트롤러가 이를 200 / 409 응답으로 분기한다.
 */
public record QueueCardRecommendationResult(
        QueueCardRecommendationResponse success,
        NoRecommendationSlotResponse noSlot
) {

    public static QueueCardRecommendationResult ofSuccess(QueueCardRecommendationResponse success) {
        return new QueueCardRecommendationResult(success, null);
    }

    public static QueueCardRecommendationResult ofNoSlot(boolean canExtendTo14Days, boolean mustChangeDuration) {
        return new QueueCardRecommendationResult(
                null, new NoRecommendationSlotResponse(canExtendTo14Days, mustChangeDuration));
    }

    public boolean hasSlots() {
        return success != null;
    }
}
