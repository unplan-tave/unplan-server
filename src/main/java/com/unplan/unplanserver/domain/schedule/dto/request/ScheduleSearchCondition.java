package com.unplan.unplanserver.domain.schedule.dto.request;

import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일정 필터 검색(GET /schedule/search) 조건. 모두 선택값이며 넘어온 것만 AND 로 조합된다.
 *
 * @param keyword       제목 부분 일치(대소문자 무시)
 * @param isQueue       true=큐 카드, false=핀 카드
 * @param statuses      진행 상태(미완료/진행중/완료) — 복수 선택 시 OR
 * @param conditionTags 컨디션 태그 — 복수 선택 시 OR
 * @param personalTags  개인 태그 이름 — 복수 선택 시 OR(하나라도 연결된 일정)
 * @param startDate     기간 필터 시작 일시(예: 2026-06-28T14:30). 카드 시간구간[날짜+시작시간~날짜+종료시간]이
 *                      [startDate, endDate]와 겹치는 핀 카드만 매칭(시간 없는 큐 카드 제외). null 이면 하한 없음
 * @param endDate       기간 필터 종료 일시. null 이면 상한 없음
 *                      startDate·endDate 를 모두 생략하면 서비스가 오늘 기준 앞뒤 3개월(날짜 기준, 큐 포함)을 기본 적용
 */
public record ScheduleSearchCondition(
        String keyword,
        Boolean isQueue,
        List<ScheduleStatus> statuses,
        List<ConditionTag> conditionTags,
        List<String> personalTags,
        LocalDateTime startDate,
        LocalDateTime endDate
) {
}
