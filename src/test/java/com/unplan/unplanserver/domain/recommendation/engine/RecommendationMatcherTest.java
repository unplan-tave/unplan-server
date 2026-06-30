package com.unplan.unplanserver.domain.recommendation.engine;

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

    // ─────────────────────────── 태그 매칭 티어 ───────────────────────────

    @Test
    @DisplayName("1순위: 정확히 일치하는 태그 카드만")
    void matchExact() {
        QueueCard c1 = card(1, CORE_TASK, 60, null, "2026-06-01T09:00");
        QueueCard c2 = card(2, BRAIN_WORK, 60, null, "2026-06-01T09:00");
        assertEquals(List.of(c1), matcher.matchByTag(CORE_TASK, List.of(c1, c2)));
    }

    @Test
    @DisplayName("2순위: 정확 일치 없으면 인접 태그 카드")
    void matchAdjacent() {
        QueueCard brain = card(2, BRAIN_WORK, 60, null, "2026-06-01T09:00"); // CORE의 인접
        QueueCard daily = card(3, DAILY_TASK, 60, null, "2026-06-01T09:00"); // CORE의 인접 아님
        assertEquals(List.of(brain), matcher.matchByTag(CORE_TASK, List.of(brain, daily)));
    }

    @Test
    @DisplayName("3순위: 정확·인접 모두 없으면 태그 무관 전체(마감 임박 폴백)")
    void matchFallbackAll() {
        QueueCard daily = card(3, DAILY_TASK, 60, null, "2026-06-01T09:00");
        QueueCard urgent = card(4, URGENT, 60, null, "2026-06-01T09:00");
        // CORE 정확 없음, 인접(BRAIN/SIMPLE) 없음 → 전체 반환
        assertEquals(List.of(daily, urgent), matcher.matchByTag(CORE_TASK, List.of(daily, urgent)));
    }

    @Test
    @DisplayName("기력 회복: 회복 태그 카드만, 없으면 빈 목록(전체로 폴백하지 않음)")
    void matchRecovery() {
        QueueCard recovery = card(1, RECOVERY, 30, null, "2026-06-01T09:00");
        QueueCard core = card(2, CORE_TASK, 60, null, "2026-06-01T09:00");
        assertEquals(List.of(recovery), matcher.matchByTag(RECOVERY, List.of(recovery, core)));
        assertTrue(matcher.matchByTag(RECOVERY, List.of(core)).isEmpty()); // 회복 카드 없음 → 빈 목록
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
