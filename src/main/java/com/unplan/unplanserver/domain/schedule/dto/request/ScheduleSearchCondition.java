package com.unplan.unplanserver.domain.schedule.dto.request;

import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 일정 필터 검색(GET /schedule/search) 조건. 모두 선택값이며 넘어온 것만 AND 로 조합된다.
 *
 * @param keyword       제목 부분 일치(대소문자 무시)
 * @param isQueue       true=큐 카드, false=핀 카드
 * @param statuses      진행 상태(미완료/진행중/완료) — 복수 선택 시 OR
 * @param conditionTags 컨디션 태그 — 복수 선택 시 OR
 * @param personalTags  개인 태그 이름 — 복수 선택 시 OR(하나라도 연결된 일정)
 * @param startDate     기간 필터 시작일(포함). Schedule.date 기준. null 이면 하한 없음
 * @param endDate       기간 필터 종료일(포함). Schedule.date 기준. null 이면 상한 없음
 * @param startTime     시간대 필터 시작시간. 카드 시간대[시작~종료]가 [startTime, endTime]과 겹치면 매칭.
 *                      시간 필터가 있으면 시간 없는 큐 카드는 제외된다. null 이면 하한 없음
 * @param endTime       시간대 필터 종료시간. null 이면 상한 없음
 */
public record ScheduleSearchCondition(
        String keyword,
        Boolean isQueue,
        List<ScheduleStatus> statuses,
        List<ConditionTag> conditionTags,
        List<String> personalTags,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime
) {
}
