package com.unplan.unplanserver.domain.recommendation.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 홈/컨디션 탭 추천 목록 응답 (GET /schedule/recommendations?date=).
 * Figma 바텀시트 구조 그대로: 빈 시간 안내("14:00 ~ 15:30까지, 1시간 동안 스케줄이 비어 있어요")
 * + 추천 카드 페이지네이션("추천 일정 1", "1/3" — PM 확정 2026-07-04, 최대 3개).
 *
 * @param date          추천 대상 날짜
 * @param conditionTag  현재 컨디션 태그 (enum 명, 예: "CORE_TASK"). 바텀시트 상단 문구용
 * @param emptyTime     추천이 배치된 빈 시간. 추천이 없으면 null
 * @param recommendations 노출 순서(displayOrder)대로 정렬된 추천 목록. 없으면 빈 배열
 */
public record RecommendationListResponse(
        LocalDate date,
        String conditionTag,
        EmptyTime emptyTime,
        List<RecommendationItem> recommendations
) {

    /** end 가 00:00 이면 '그날의 끝(24:00)'을 뜻한다 (EmptyTimeFinder 경계 규약) */
    public record EmptyTime(
            LocalTime startTime,
            LocalTime endTime,
            int durationMinutes
    ) {}

    /**
     * @param estimatedTime 소요시간(분). Figma 카드의 "약 30분 소요"
     * @param deadline      원본 큐 카드의 마감일. 없으면 null → "마감일 없음"
     * @param sourceType    QUEUE_CARD / RECOVERY_MEAN
     */
    public record RecommendationItem(
            Long recommendId,
            String title,
            LocalTime startTime,
            LocalTime endTime,
            Integer estimatedTime,
            LocalDate deadline,
            String conditionTag,
            String sourceType,
            int displayOrder
    ) {}
}
