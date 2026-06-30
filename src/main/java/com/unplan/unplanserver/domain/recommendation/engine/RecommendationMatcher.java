package com.unplan.unplanserver.domain.recommendation.engine;

import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static com.unplan.unplanserver.domain.schedule.enums.ConditionTag.*;

/**
 * 현재 컨디션 태그를 기준으로 큐 카드를 선별·정렬한다 (Notion "추천 로직" 3·4단계).
 * 현재 컨디션 태그는 외부(컨디션 점수 모듈)에서 산출되어 입력으로 주입된다 — 이 클래스는 비의존.
 */
@Component
public class RecommendationMatcher {

    /**
     * 인접 태그 표 (Notion 그대로 하드코딩).
     * 인접 기준은 '강도'가 아니라 '현재 상태와의 일치 근접도'라 일상작업→두뇌/단순 같은 매핑도 의도된 것.
     * 강도 순서로 재해석/수정하지 말 것. 기력 회복은 별도 분기라 표에 없음.
     */
    private static final Map<ConditionTag, List<ConditionTag>> ADJACENT = Map.of(
            CORE_TASK, List.of(BRAIN_WORK, SIMPLE_TASK),
            BRAIN_WORK, List.of(DAILY_TASK),
            SIMPLE_TASK, List.of(DAILY_TASK),
            DAILY_TASK, List.of(BRAIN_WORK, SIMPLE_TASK),
            URGENT, List.of(DAILY_TASK)
    );

    public List<ConditionTag> adjacentTags(ConditionTag current) {
        return ADJACENT.getOrDefault(current, List.of());
    }

    /**
     * 현재 컨디션 태그로 추천 후보 큐 카드를 선별한다.
     * - 기력 회복: 회복(RECOVERY) 태그 카드만. (없을 때의 '온보딩 회복 수단' 폴백은 온보딩 데이터가 필요해 서비스 계층에서 처리)
     * - 그 외: 1순위 정확 일치 → (없으면) 2순위 인접 태그 → (없으면) 3순위 태그 무관 전체(마감 임박 폴백)
     */
    public List<QueueCard> matchByTag(ConditionTag current, List<QueueCard> cards) {
        if (current == RECOVERY) {
            return cards.stream().filter(c -> c.conditionTag() == RECOVERY).toList();
        }

        List<QueueCard> exact = cards.stream().filter(c -> c.conditionTag() == current).toList();
        if (!exact.isEmpty()) return exact;

        List<ConditionTag> adjacent = adjacentTags(current);
        List<QueueCard> adj = cards.stream().filter(c -> adjacent.contains(c.conditionTag())).toList();
        if (!adj.isEmpty()) return adj;

        return cards; // 3순위: 태그 무관 전체 (정렬 단계에서 마감 임박순 적용)
    }

    /**
     * 큐 카드 정렬 (Notion 4단계).
     * 1순위: 마감일 임박순 — 마감일 있는 카드를 오름차순 우선, 없는 카드는 후순위(생성일 오름차순)
     * 2순위: 소요시간 적합순 — 빈 시간(slotLengthMinutes)과 소요시간 차이가 작은 순. slotLength 가 null 이면 생략.
     */
    public List<QueueCard> sort(List<QueueCard> cards, Integer slotLengthMinutes) {
        return cards.stream().sorted(deadlineThenFit(slotLengthMinutes)).toList();
    }

    private Comparator<QueueCard> deadlineThenFit(Integer slotLengthMinutes) {
        return (a, b) -> {
            boolean aHas = a.deadline() != null;
            boolean bHas = b.deadline() != null;

            // 마감일 있는 카드가 항상 먼저
            if (aHas != bHas) return aHas ? -1 : 1;

            if (aHas) {
                int c = a.deadline().compareTo(b.deadline());
                if (c != 0) return c;
            } else {
                int c = nullsLastCompare(a.createdAt(), b.createdAt());
                if (c != 0) return c;
            }

            // 2순위: 소요시간 적합순 (slotLength 기준 차이 작은 순)
            if (slotLengthMinutes != null) {
                return Integer.compare(fitDiff(a, slotLengthMinutes), fitDiff(b, slotLengthMinutes));
            }
            return 0;
        };
    }

    private int fitDiff(QueueCard card, int slotLengthMinutes) {
        if (card.estimatedMinutes() == null) return Integer.MAX_VALUE; // 미정은 가장 후순위
        return Math.abs(slotLengthMinutes - card.estimatedMinutes());
    }

    private int nullsLastCompare(LocalDateTime a, LocalDateTime b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }
}
