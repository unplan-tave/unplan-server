package com.unplan.unplanserver.domain.recommendation.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 큐 카드 7일(확장 시 14일) 추천 시간대 목록 응답
 * (GET /schedule/{scheduleId}/recommendations).
 *
 * Figma: 큐카드 상세 → "추천 시간 확인하기" → 화살표로 후보 시간대를 넘겨보며 핀 전환.
 * 각 후보는 {@code recommendation} 행으로 영속화되어 recommendId 로 기존 수락 API
 * (POST /schedule/recommendations/{recommendId}/accept)를 그대로 태워 핀 카드로 전환한다.
 *
 * 후보는 날짜별 1개(가장 이른 빈 시간)로 최대 rangeDays 개, 날짜·시작시각 오름차순이다
 * (Notion 정렬 규칙: "7일은 날짜·시작시각").
 *
 * @param scheduleId    추천 대상 큐 카드 id
 * @param title         큐 카드 제목 (후보 카드에 공통 표시)
 * @param estimatedTime 소요시간(분). 모든 후보 슬롯 길이의 기준 (Notion: "기존과 동일한 소요 시간")
 * @param rangeDays     탐색 범위(일). 7 또는 14
 * @param slots         추천 시간대 후보 목록 (displayOrder 순)
 */
public record QueueCardRecommendationResponse(
        Long scheduleId,
        String title,
        Integer estimatedTime,
        int rangeDays,
        List<Slot> slots
) {

    /**
     * 추천 시간대 후보 한 건. endTime 이 00:00 이면 '그날의 끝(24:00)'을 뜻한다
     * (EmptyTimeFinder 경계 규약). 수락 시 핀 카드로는 23:59 로 클램핑된다.
     */
    public record Slot(
            Long recommendId,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            int displayOrder
    ) {}
}
