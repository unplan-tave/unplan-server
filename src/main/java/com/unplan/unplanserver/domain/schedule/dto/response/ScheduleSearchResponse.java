package com.unplan.unplanserver.domain.schedule.dto.response;

import com.unplan.unplanserver.domain.schedule.entity.Schedule;

import java.util.List;

/**
 * 일정 필터 검색 결과 항목 (카드 리스트 카드 1개).
 * 날짜·시간은 스케줄 도메인 응답 컨벤션에 맞춰 String(ISO) 으로 직렬화한다.
 *
 * @param date          핀=시작일, 큐=마감일 (Schedule.date). 여러 날 걸친 핀은 endDate 가 마감일.
 *                      카드 리스트 정렬 기준은 마감일 = coalesce(endDate, date) (ScheduleSpecifications 참고)
 * @param isRecommended 추천으로 수락되어 생성된 일정인지 (Recommendation.acceptedScheduleId 매핑)
 */
public record ScheduleSearchResponse(
        Long scheduleId,
        String title,
        String date,
        String endDate,
        String startTime,
        String endTime,
        Integer estimatedTime,
        String conditionTag,
        List<String> personalTags,
        String status,
        boolean isRecommended,
        boolean isConflict
) {

    public static ScheduleSearchResponse of(Schedule s, List<String> personalTags, boolean isRecommended) {
        return new ScheduleSearchResponse(
                s.getScheduleId(),
                s.getTitle(),
                s.getDate() != null ? s.getDate().toString() : null,
                s.getEndDate() != null ? s.getEndDate().toString() : null,
                s.getStartTime() != null ? s.getStartTime().toString() : null,
                s.getEndTime() != null ? s.getEndTime().toString() : null,
                s.getEstimatedTime(),
                s.getConditionTag() != null ? s.getConditionTag().name() : null,
                personalTags,
                s.getStatus() != null ? s.getStatus().name() : null,
                isRecommended,
                Boolean.TRUE.equals(s.getIsConflict())
        );
    }
}
