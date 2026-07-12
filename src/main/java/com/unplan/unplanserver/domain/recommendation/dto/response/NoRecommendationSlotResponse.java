package com.unplan.unplanserver.domain.recommendation.dto.response;

/**
 * 큐 카드 추천 시간대를 찾지 못했을 때의 409 응답 body
 * (GET /schedule/{scheduleId}/recommendations).
 *
 * Figma 분기:
 * <ul>
 *   <li>7일 내 없음 → {@code canExtendTo14Days=true} ("14일 이내의 추천 시간대를 찾아볼까요?"):
 *       프론트가 {@code ?days=14} 로 재요청.</li>
 *   <li>14일까지도 없음, 또는 소요시간 미정 → {@code mustChangeDuration=true}
 *       ("소요시간을 줄이면 시간대를 더 찾아볼 수 있어요" — 소요시간 변경만 가능).</li>
 * </ul>
 *
 * @param canExtendTo14Days 7일 탐색 결과가 비어 14일 확장 재요청이 가능한지
 * @param mustChangeDuration 확장으로도 해결 불가 → 소요시간 변경이 유일한 선택지인지
 */
public record NoRecommendationSlotResponse(
        boolean canExtendTo14Days,
        boolean mustChangeDuration
) {}
