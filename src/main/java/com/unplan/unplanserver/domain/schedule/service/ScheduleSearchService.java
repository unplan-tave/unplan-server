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

    // 카드 리스트 정렬(최신순 = 마감일 내림차순)은 coalesce(endDate, date) 기반이라 Sort 로 표현 불가 →
    // ScheduleSpecifications.search 안에서 orderBy 로 지정한다. (Figma 기획: 카드리스트 정렬은 '최신순'만 존재)
    private final ScheduleRepository scheduleRepository;
    private final SchedulePersonalTagRepository schedulePersonalTagRepository;
    private final RecommendationRepository recommendationRepository;

    @Transactional(readOnly = true)
    public PageResponse<ScheduleSearchResponse> search(Long memberId, ScheduleSearchCondition condition, Integer page) {
        // 기간필터 기본값(오늘 ±3개월)·datetime 구간 매칭은 ScheduleSpecifications 가 처리한다.
        Specification<Schedule> spec = ScheduleSpecifications.search(memberId, condition);
        PageRequest pageRequest = PagingUtils.pageRequest(page);

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
}
