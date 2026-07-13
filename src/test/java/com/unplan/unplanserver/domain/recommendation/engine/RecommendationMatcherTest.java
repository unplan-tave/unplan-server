package com.unplan.unplanserver.domain.recommendation.engine;

import com.unplan.unplanserver.domain.recommendation.engine.RecommendationMatcher.MatchedCard;
import com.unplan.unplanserver.domain.recommendation.enums.MatchTier;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.unplan.unplanserver.domain.schedule.enums.ConditionTag.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 컨디션 태그 매칭/정렬(RecommendationMatcher) 순수 로직 검증.
 */
class RecommendationMatcherTest {

    private final RecommendationMatcher matcher = new RecommendationMatcher();

    private QueueCard card(long id, ConditionTag tag, Integer est, String deadline, String createdAt) {
        return new QueueCard(id, tag, est,
                deadline == null ? null : LocalDate.parse(deadline),
                createdAt == null ? null : LocalDateTime.parse(createdAt));
    }

    // ─────────────────────────── 인접 태그 표 ───────────────────────────

    @Test
    @DisplayName("인접 태그 표는 Notion 정의 그대로 (강도 재해석 아님)")
    void adjacentTags() {
        assertEquals(List.of(BRAIN_WORK, SIMPLE_TASK), matcher.adjacentTags(CORE_TASK));
        assertEquals(List.of(DAILY_TASK), matcher.adjacentTags(BRAIN_WORK));
        assertEquals(List.of(DAILY_TASK), matcher.adjacentTags(SIMPLE_TASK));
        assertEquals(List.of(BRAIN_WORK, SIMPLE_TASK), matcher.adjacentTags(DAILY_TASK));
        assertEquals(List.of(DAILY_TASK), matcher.adjacentTags(URGENT));
        assertTrue(matcher.adjacentTags(RECOVERY).isEmpty()); // 기력 회복은 별도 분기
    }

    // ─────────────────────────── 태그 매칭 티어(교차 채우기) ───────────────────────────

    @Test
    @DisplayName("정확 일치 티어를 먼저, 부족하면 인접→나머지 순으로 이어 채운다 (총 limit)")
    void fillsAcrossTiers() {
        QueueCard exact = card(1, CORE_TASK, 60, null, "2026-06-01T09:00");
        QueueCard brain = card(2, BRAIN_WORK, 60, null, "2026-06-01T09:00"); // CORE 인접
        QueueCard daily = card(3, DAILY_TASK, 60, null, "2026-06-01T09:00"); // CORE 인접 아님 → 나머지 티어
        // limit 3 → 정확(exact) → 인접(brain) → 나머지(daily), 각 카드에 뽑힌 티어가 함께 실린다
        assertEquals(List.of(
                        new MatchedCard(exact, MatchTier.EXACT),
                        new MatchedCard(brain, MatchTier.ADJACENT),
                        new MatchedCard(daily, MatchTier.DEADLINE)),
                matcher.match(CORE_TASK, List.of(daily, brain, exact), 3, null));
    }

    @Test
    @DisplayName("정확 일치가 limit 이상이면 그 안에서 상위 limit개만, 하위 티어는 노출하지 않는다")
    void exactTierCapsWithoutLowerTiers() {
        QueueCard e1 = card(1, CORE_TASK, 60, "2026-07-01", "2026-06-01T09:00");
        QueueCard e2 = card(2, CORE_TASK, 60, "2026-07-02", "2026-06-01T09:00");
        QueueCard e3 = card(3, CORE_TASK, 60, "2026-07-03", "2026-06-01T09:00");
        QueueCard adj = card(4, BRAIN_WORK, 60, "2026-06-30", "2026-06-01T09:00"); // 인접·마감 더 임박하나 하위 티어라 제외
        // 정확 3개 → 마감순 e1,e2,e3. 인접(adj)은 티어 우선순위에 밀려 제외 (마감 임박이어도)
        assertEquals(List.of(e1, e2, e3),
                matcher.match(CORE_TASK, List.of(e1, e2, e3, adj), 3, null).stream()
                        .map(MatchedCard::card).toList());
    }

    @Test
    @DisplayName("보여줄 수 있는 후보가 limit 미만이면 있는 만큼만 반환")
    void fewerThanLimit() {
        QueueCard exact = card(1, CORE_TASK, 60, null, "2026-06-01T09:00");
        assertEquals(List.of(new MatchedCard(exact, MatchTier.EXACT)),
                matcher.match(CORE_TASK, List.of(exact), 3, null));
    }

    @Test
    @DisplayName("기력 회복: 회복 태그 카드만 채우고 없으면 빈 목록(다른 태그로 폴백하지 않음)")
    void matchRecovery() {
        QueueCard recovery = card(1, RECOVERY, 30, null, "2026-06-01T09:00");
        QueueCard core = card(2, CORE_TASK, 60, null, "2026-06-01T09:00");
        // 회복 태그 카드는 정확 일치(EXACT) 취급
        assertEquals(List.of(new MatchedCard(recovery, MatchTier.EXACT)),
                matcher.match(RECOVERY, List.of(recovery, core), 3, null));
        assertTrue(matcher.match(RECOVERY, List.of(core), 3, null).isEmpty()); // 회복 카드 없음 → 빈 목록
    }

    // ─────────────────────────── 정렬 ───────────────────────────

    @Test
    @DisplayName("정렬 1순위: 마감일 오름차순, 마감일 없는 카드는 후순위(생성일 오름차순)")
    void sortByDeadlineThenCreated() {
        QueueCard a = card(1, CORE_TASK, 60, "2026-07-03", "2026-06-01T09:00");
        QueueCard b = card(2, CORE_TASK, 60, "2026-07-01", "2026-06-01T09:00");
        QueueCard c = card(3, CORE_TASK, 60, null, "2026-06-01T08:00"); // 생성 이른
        QueueCard d = card(4, CORE_TASK, 60, null, "2026-06-02T08:00"); // 생성 늦음
        assertEquals(List.of(b, a, c, d), matcher.sort(List.of(a, b, c, d), null));
    }

    @Test
    @DisplayName("정렬 2순위: 같은 마감일이면 소요시간이 빈 시간에 더 맞는 카드 우선")
    void sortByFitWhenSameDeadline() {
        QueueCard near = card(1, CORE_TASK, 55, "2026-07-01", "2026-06-01T09:00"); // |60-55|=5
        QueueCard far = card(2, CORE_TASK, 30, "2026-07-01", "2026-06-01T09:00");  // |60-30|=30
        assertEquals(List.of(near, far), matcher.sort(List.of(far, near), 60));
    }
}
