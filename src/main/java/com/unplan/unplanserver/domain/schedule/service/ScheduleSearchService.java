package com.unplan.unplanserver.domain.schedule.service;

import com.unplan.unplanserver.domain.recommendation.repository.RecommendationRepository;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleSearchCondition;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleSearchResponse;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleSpecifications;
import com.unplan.unplanserver.domain.schedule.repository.SchedulePersonalTagRepository;
import com.unplan.unplanserver.global.response.PageResponse;
import com.unplan.unplanserver.global.response.PagingUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 일정 필터 검색(GET /schedule/search). 저장된 일정 행을 필터·정렬해 offset 페이지네이션(size 30)으로 반환한다.
 * 반복 일정은 원본 1건으로만 노출한다(인스턴스 미확장 → 유한 카운트).
 */
@Service
@RequiredArgsConstructor
public class ScheduleSearchService {

    // 카드 리스트 기본 정렬: 최신순 = 날짜 내림차순(핀=시작일, 큐=마감일 — 둘 다 date). 동일 날짜는 id 내림차순으로 안정 정렬.
    // (Figma 기획: 카드리스트 정렬은 '최신순'만 존재)
    private static final Sort SORT = Sort.by(
            Sort.Order.desc("date").nullsLast(),
            Sort.Order.desc("scheduleId"));

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    // 기간필터 미전송 시 기본 조회 범위: 오늘 기준 앞뒤 3개월(총 6개월). 카드 리스트가 무한 로딩되지 않도록.
    private static final int DEFAULT_RANGE_MONTHS = 3;

    private final ScheduleRepository scheduleRepository;
    private final SchedulePersonalTagRepository schedulePersonalTagRepository;
    private final RecommendationRepository recommendationRepository;

    @Transactional(readOnly = true)
    public PageResponse<ScheduleSearchResponse> search(Long memberId, ScheduleSearchCondition condition, Integer page) {
        // 기간필터를 전혀 안 보내면(양쪽 다 null) 오늘 기준 앞뒤 3개월을 기본 범위로 적용.
        // 한쪽이라도 보내면 사용자가 명시한 필터로 보고 그대로 존중한다.
        ScheduleSearchCondition effective = applyDefaultDateRange(condition);
        Specification<Schedule> spec = ScheduleSpecifications.search(memberId, effective);
        PageRequest pageRequest = PagingUtils.pageRequest(page, SORT);

        Page<Schedule> found = scheduleRepository.findAll(spec, pageRequest);
        if (found.isEmpty()) {
            return PageResponse.of(List.of(), found);
        }

        List<Schedule> schedules = found.getContent();
        List<Long> scheduleIds = schedules.stream().map(Schedule::getScheduleId).toList();

        // 개인 태그·추천 여부를 각각 한 번에 조회해 매핑 (건별 조회 N+1 방지)
        Map<Long, List<String>> tagsByScheduleId = schedulePersonalTagRepository
                .findTagRowsByScheduleIds(scheduleIds).stream()
                .collect(Collectors.groupingBy(
                        SchedulePersonalTagRepository.ScheduleTagRow::getScheduleId,
                        Collectors.mapping(SchedulePersonalTagRepository.ScheduleTagRow::getTagName, Collectors.toList())));
        Set<Long> recommendedIds = Set.copyOf(
                recommendationRepository.findAcceptedScheduleIds(memberId, scheduleIds));

        List<ScheduleSearchResponse> items = schedules.stream()
                .map(s -> ScheduleSearchResponse.of(
                        s,
                        tagsByScheduleId.getOrDefault(s.getScheduleId(), List.of()),
                        recommendedIds.contains(s.getScheduleId())))
                .toList();

        return PageResponse.of(items, found);
    }

    /**
     * 기간필터가 전혀 없으면(startDate·endDate 둘 다 null) 오늘 기준 앞뒤 3개월을 기본 범위로 채운다.
     * 한쪽이라도 지정돼 있으면 사용자가 명시한 조건으로 보고 그대로 둔다.
     */
    private ScheduleSearchCondition applyDefaultDateRange(ScheduleSearchCondition c) {
        if (c.startDate() != null || c.endDate() != null) {
            return c;
        }
        LocalDate today = LocalDate.now(KST_ZONE_ID);
        return new ScheduleSearchCondition(
                c.keyword(), c.isQueue(), c.statuses(), c.conditionTags(), c.personalTags(),
                today.minusMonths(DEFAULT_RANGE_MONTHS), today.plusMonths(DEFAULT_RANGE_MONTHS));
    }
}
