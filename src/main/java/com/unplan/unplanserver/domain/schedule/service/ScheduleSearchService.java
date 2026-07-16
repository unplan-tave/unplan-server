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

    // 카드 리스트 기본 정렬: 날짜 오름차순(핀=시작일, 큐=마감일 — 둘 다 date). 동일 날짜는 id 로 안정 정렬.
    private static final Sort SORT = Sort.by(
            Sort.Order.asc("date").nullsLast(),
            Sort.Order.asc("scheduleId"));

    private final ScheduleRepository scheduleRepository;
    private final SchedulePersonalTagRepository schedulePersonalTagRepository;
    private final RecommendationRepository recommendationRepository;

    @Transactional(readOnly = true)
    public PageResponse<ScheduleSearchResponse> search(Long memberId, ScheduleSearchCondition condition, Integer page) {
        Specification<Schedule> spec = ScheduleSpecifications.search(memberId, condition);
        PageRequest pageRequest = PagingUtils.pageRequest(page, SORT);

        Page<Schedule> found = scheduleRepository.findAll(spec, pageRequest);
        if (found.isEmpty()) {
            return PageResponse.of(List.of(), found);
        }

        List<Long> scheduleIds = found.getContent().stream().map(Schedule::getScheduleId).toList();

        // 개인 태그·추천 여부를 각각 한 번에 조회해 매핑 (건별 조회 N+1 방지)
        Map<Long, List<String>> tagsByScheduleId = schedulePersonalTagRepository
                .findTagRowsByScheduleIds(scheduleIds).stream()
                .collect(Collectors.groupingBy(
                        SchedulePersonalTagRepository.ScheduleTagRow::getScheduleId,
                        Collectors.mapping(SchedulePersonalTagRepository.ScheduleTagRow::getTagName, Collectors.toList())));
        Set<Long> recommendedIds = Set.copyOf(
                recommendationRepository.findAcceptedScheduleIds(memberId, scheduleIds));

        List<ScheduleSearchResponse> items = found.getContent().stream()
                .map(s -> ScheduleSearchResponse.of(
                        s,
                        tagsByScheduleId.getOrDefault(s.getScheduleId(), List.of()),
                        recommendedIds.contains(s.getScheduleId())))
                .toList();

        return PageResponse.of(items, found);
    }
}
