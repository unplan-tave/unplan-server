package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleSearchCondition;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.entity.SchedulePersonalTag;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 일정 필터 검색용 동적 조건 조합. 넘어온 필터만 AND 로 붙인다.
 * 개인 태그는 조인 테이블 EXISTS 서브쿼리로 판정한다(하나라도 연결되면 매칭).
 */
public final class ScheduleSpecifications {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");
    // 기간필터 미전송 시 기본 조회 범위: 오늘 기준 앞뒤 3개월(총 6개월). 카드 리스트가 무한 로딩되지 않도록.
    private static final int DEFAULT_RANGE_MONTHS = 3;

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
            // 기간 필터 (datetime 구간):
            // - startDate/endDate(일시)가 지정되면:
            //   핀 카드 → 시간구간[날짜+시작시간 ~ 날짜+종료시간]이 [startDate, endDate]와 겹치는 카드 매칭
            //   큐 카드 → 마감일(date) 기준으로 [startDate.toLocalDate(), endDate.toLocalDate()] 범위 매칭
            // - 둘 다 없으면: 오늘 기준 앞뒤 3개월을 Schedule.date 기준으로 적용한다(기본 카드리스트 — 큐 카드 포함).
            if (c.startDate() != null || c.endDate() != null) {
                // 핀 카드: 시간구간 overlap
                List<Predicate> pinPredicates = new ArrayList<>();
                pinPredicates.add(cb.isNotNull(root.get("startTime")));
                if (c.startDate() != null) {
                    LocalDate sd = c.startDate().toLocalDate();
                    LocalTime st = c.startDate().toLocalTime();
                    pinPredicates.add(cb.or(
                            cb.greaterThan(cb.<LocalDate>coalesce()
                                    .value(root.get("endDate")).value(root.get("date")), sd),
                            cb.and(
                                    cb.equal(cb.<LocalDate>coalesce()
                                            .value(root.get("endDate")).value(root.get("date")), sd),
                                    cb.greaterThan(root.get("endTime"), st))));
                }
                if (c.endDate() != null) {
                    LocalDate ed = c.endDate().toLocalDate();
                    LocalTime et = c.endDate().toLocalTime();
                    pinPredicates.add(cb.or(
                            cb.lessThan(root.get("date"), ed),
                            cb.and(cb.equal(root.get("date"), ed), cb.lessThan(root.get("startTime"), et))));
                }

                // 큐 카드: 마감일(date) 기준
                List<Predicate> queuePredicates = new ArrayList<>();
                queuePredicates.add(cb.isTrue(root.get("isQueue")));
                if (c.startDate() != null) {
                    queuePredicates.add(cb.or(
                            cb.isNull(root.get("date")),
                            cb.greaterThanOrEqualTo(root.get("date"), c.startDate().toLocalDate())));
                }
                if (c.endDate() != null) {
                    queuePredicates.add(cb.or(
                            cb.isNull(root.get("date")),
                            cb.lessThanOrEqualTo(root.get("date"), c.endDate().toLocalDate())));
                }

                predicates.add(cb.or(
                        cb.and(pinPredicates.toArray(new Predicate[0])),
                        cb.and(queuePredicates.toArray(new Predicate[0]))));
            } else {
                // 기간필터 미전송 → 오늘(KST) 기준 앞뒤 3개월 기본 범위(날짜 기준, 큐 카드 포함).
                // 단, 마감일(date) 없는 큐 카드는 기간 범위로 걸러낼 수 없으므로 항상 노출한다.
                LocalDate today = LocalDate.now(KST_ZONE_ID);
                predicates.add(cb.or(
                        cb.isNull(root.get("date")),
                        cb.and(
                                cb.greaterThanOrEqualTo(
                                        cb.<LocalDate>coalesce().value(root.get("endDate")).value(root.get("date")),
                                        today.minusMonths(DEFAULT_RANGE_MONTHS)),
                                cb.lessThanOrEqualTo(root.get("date"), today.plusMonths(DEFAULT_RANGE_MONTHS)))));
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

            // 카드 리스트 정렬: '마감일' 기준 최신순.
            // 여러 날 걸친 핀 카드는 시작일(date)이 아니라 마감일(endDate)에 놓여야 하므로 coalesce(endDate, date)로 정렬한다.
            // (단일 핀·큐 카드는 endDate 가 없어 date 로 폴백 — 큐 카드의 date 는 마감일). 동일 마감일은 id 내림차순으로 안정 정렬.
            // 마감일 없는 카드(coalesce=null)는 맨 뒤(NULLS LAST). coalesce 정렬은 Sort 로 표현 불가라 여기서 지정한다.
            // count 쿼리(resultType=Long)에는 order by 를 넣지 않는다.
            Class<?> resultType = query.getResultType();
            if (resultType != Long.class && resultType != long.class) {
                Expression<LocalDate> deadline = cb.<LocalDate>coalesce()
                        .value(root.get("endDate")).value(root.get("date"));
                query.orderBy(
                        ((HibernateCriteriaBuilder) cb).desc(deadline, false), // nullsFirst=false → NULLS LAST
                        cb.desc(root.get("scheduleId")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
