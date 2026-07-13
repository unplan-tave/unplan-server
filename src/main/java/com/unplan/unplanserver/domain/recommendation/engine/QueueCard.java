package com.unplan.unplanserver.domain.recommendation.engine;

import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 추천 대상이 되는 큐 카드의 최소 정보. Schedule 엔티티에서 매핑한다.
 * (마감일 = Schedule.date, 소요시간 = estimatedTime)
 * 추천 매칭/정렬 로직이 엔티티에 직접 의존하지 않도록 분리한 값 타입.
 */
public record QueueCard(
        Long scheduleId,
        ConditionTag conditionTag,
        Integer estimatedMinutes, // 소요시간(분). 미정이면 빈 시간 필터 단계에서 이미 제외됨
        LocalDate deadline,       // 마감일. 없을 수 있음
        LocalDateTime createdAt
) {}
