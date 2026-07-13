package com.unplan.unplanserver.domain.recommendation.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 추천 수락 응답 (POST /schedule/recommendations/{recommendId}/accept).
 * 수락은 큐 카드를 핀 카드로 '전환'하거나(회복 수단이면 새 일정 생성) 그 결과 일정을 돌려준다.
 *
 * @param recommendId 수락 처리된 추천 id
 * @param scheduleId  전환/생성된 핀 카드(Schedule) id
 * @param created     true=회복 수단이라 새 일정 생성, false=기존 큐 카드를 핀 카드로 전환
 */
public record RecommendationAcceptResponse(
        Long recommendId,
        Long scheduleId,
        String title,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String sourceType,
        boolean created
) {}
