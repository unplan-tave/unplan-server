package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleSearchCondition;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.entity.SchedulePersonalTag;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 일정 필터 검색용 동적 조건 조합. 넘어온 필터만 AND 로 붙인다.
 * 개인 태그는 조인 테이블 EXISTS 서브쿼리로 판정한다(하나라도 연결되면 매칭).
 */
public final class ScheduleSpecifications {

    private ScheduleSpecifications() {
    }

    public static Specification<Schedule> search(Long memberId, ScheduleSearchCondition c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("memberId"), memberId));

            if (StringUtils.hasText(c.keyword())) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + c.keyword().trim().toLowerCase() + "%"));
            }
            if (c.isQueue() != null) {
                predicates.add(cb.equal(root.get("isQueue"), c.isQueue()));
            }
            if (!CollectionUtils.isEmpty(c.statuses())) {
                predicates.add(root.get("status").in(c.statuses()));
            }
            if (!CollectionUtils.isEmpty(c.conditionTags())) {
                predicates.add(root.get("conditionTag").in(c.conditionTags()));
            }
            // 기간 필터: Schedule.date 가 [startDate, endDate] 양끝 포함. 한쪽만 오면 그 방향만 제한.
            if (c.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), c.startDate()));
            }
            if (c.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), c.endDate()));
            }
            // 시간대 필터: 카드 시간대[startTime, endTime]가 필터 시간대[c.startTime, c.endTime]와 겹치면 매칭(overlap).
            // 시간이 없는 큐 카드는 제외한다. overlap = cardStart < filterEnd AND cardEnd > filterStart 로 분해.
            if (c.startTime() != null || c.endTime() != null) {
                predicates.add(cb.isNotNull(root.get("startTime"))); // 시간 없는 큐 카드 제외
                if (c.startTime() != null) {
                    predicates.add(cb.greaterThan(root.get("endTime"), c.startTime()));   // 카드가 필터 시작 이후까지 지속
                }
                if (c.endTime() != null) {
                    predicates.add(cb.lessThan(root.get("startTime"), c.endTime()));       // 카드가 필터 종료 이전에 시작
                }
            }
            if (!CollectionUtils.isEmpty(c.personalTags())) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<SchedulePersonalTag> spt = sub.from(SchedulePersonalTag.class);
                sub.select(cb.literal(1L));
                sub.where(
                        cb.equal(spt.get("schedule"), root),
                        spt.get("personalTag").get("name").in(c.personalTags())
                );
                predicates.add(cb.exists(sub));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
