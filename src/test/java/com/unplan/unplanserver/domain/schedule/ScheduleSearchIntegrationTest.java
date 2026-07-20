package com.unplan.unplanserver.domain.schedule;

import com.unplan.unplanserver.domain.recommendation.entity.Recommendation;
import com.unplan.unplanserver.domain.recommendation.enums.RecommendationSourceType;
import com.unplan.unplanserver.domain.recommendation.repository.RecommendationRepository;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleSearchCondition;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleSearchResponse;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unplan.unplanserver.domain.schedule.service.ScheduleSearchService;
import com.unplan.unplanserver.domain.schedule.service.TagService;
import com.unplan.unplanserver.global.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleSearchIntegrationTest {

    @Autowired private ScheduleSearchService searchService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RecommendationRepository recommendationRepository;
    @Autowired private TagService tagService;
    @Autowired private ObjectMapper objectMapper;

    private static final Long MEMBER_ID = 9001L;
    private static final Long OTHER_MEMBER = 9002L;

    private ScheduleSearchCondition cond(String keyword, Boolean isQueue,
                                         List<ScheduleStatus> statuses, List<ConditionTag> tags, List<String> personalTags) {
        return new ScheduleSearchCondition(keyword, isQueue, statuses, tags, personalTags, null, null);
    }

    private ScheduleSearchCondition dateRange(LocalDate startDate, LocalDate endDate) {
        return new ScheduleSearchCondition(null, null, null, null, null, startDate, endDate);
    }

    private ScheduleSearchCondition empty() {
        return cond(null, null, null, null, null);
    }

    @Test
    @DisplayName("필터 없으면 본인 일정만 날짜 오름차순으로 반환한다")
    void noFilterReturnsOwnSortedByDate() {
        pin("회의", LocalDate.of(2026, 6, 3), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("운동", LocalDate.of(2026, 6, 1), ScheduleStatus.DONE, ConditionTag.RECOVERY);
        queue("과제", LocalDate.of(2026, 6, 2), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("남의 일정", LocalDate.of(2026, 6, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK, OTHER_MEMBER);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID, empty(), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title)
                .containsExactly("운동", "과제", "회의"); // 6/1, 6/2, 6/3 — 남의 것 제외
        assertThat(res.pagination().totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("keyword 는 제목 부분일치(대소문자 무시)")
    void keywordFiltersByTitle() {
        pin("Team Meeting", LocalDate.of(2026, 6, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("점심 약속", LocalDate.of(2026, 6, 2), ScheduleStatus.TODO, ConditionTag.DAILY_TASK);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID, cond("meeting", null, null, null, null), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title).containsExactly("Team Meeting");
    }

    @Test
    @DisplayName("isQueue·status·conditionTag 필터가 AND 로 좁힌다")
    void combinedFiltersAnd() {
        queue("큐-할일-코어", LocalDate.of(2026, 6, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        queue("큐-완료-코어", LocalDate.of(2026, 6, 2), ScheduleStatus.DONE, ConditionTag.CORE_TASK);
        pin("핀-할일-코어", LocalDate.of(2026, 6, 3), ScheduleStatus.TODO, ConditionTag.CORE_TASK);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID,
                cond(null, true, List.of(ScheduleStatus.TODO), List.of(ConditionTag.CORE_TASK), null), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title).containsExactly("큐-할일-코어");
    }

    @Test
    @DisplayName("status 복수 지정은 OR 로 매칭한다")
    void statusMultiOr() {
        pin("할일", LocalDate.of(2026, 6, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("진행중", LocalDate.of(2026, 6, 2), ScheduleStatus.IN_PROGRESS, ConditionTag.CORE_TASK);
        pin("완료", LocalDate.of(2026, 6, 3), ScheduleStatus.DONE, ConditionTag.CORE_TASK);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID,
                cond(null, null, List.of(ScheduleStatus.TODO, ScheduleStatus.IN_PROGRESS), null, null), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title).containsExactly("할일", "진행중");
    }

    @Test
    @DisplayName("기간 필터는 일정 날짜 기준 양끝 포함이며, 한쪽만 주면 그 방향만 제한한다")
    void dateRangeFilterInclusiveAndOpenEnded() {
        pin("5월", LocalDate.of(2026, 5, 31), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("6월시작", LocalDate.of(2026, 6, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("6월중순", LocalDate.of(2026, 6, 15), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("6월끝", LocalDate.of(2026, 6, 30), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("7월", LocalDate.of(2026, 7, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);

        // 양끝 포함 [6/1, 6/30]
        assertThat(searchService.search(MEMBER_ID,
                dateRange(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)), 0).data())
                .extracting(ScheduleSearchResponse::title)
                .containsExactly("6월시작", "6월중순", "6월끝");

        // startDate 만 → 그 날짜 이후 전부
        assertThat(searchService.search(MEMBER_ID,
                dateRange(LocalDate.of(2026, 6, 30), null), 0).data())
                .extracting(ScheduleSearchResponse::title)
                .containsExactly("6월끝", "7월");

        // endDate 만 → 그 날짜 이전 전부
        assertThat(searchService.search(MEMBER_ID,
                dateRange(null, LocalDate.of(2026, 6, 1)), 0).data())
                .extracting(ScheduleSearchResponse::title)
                .containsExactly("5월", "6월시작");
    }

    @Test
    @DisplayName("personalTags 는 하나라도 연결된 일정을 매칭하고, 응답에 태그가 담긴다")
    void personalTagsOrAndAppearInResponse() {
        Schedule a = pin("보고서", LocalDate.of(2026, 6, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        tagService.attachTags(a, MEMBER_ID, List.of("업무", "중요"));
        Schedule b = pin("산책", LocalDate.of(2026, 6, 2), ScheduleStatus.TODO, ConditionTag.RECOVERY);
        tagService.attachTags(b, MEMBER_ID, List.of("휴식"));

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID,
                cond(null, null, null, null, List.of("업무")), 0);

        assertThat(res.data()).hasSize(1);
        ScheduleSearchResponse item = res.data().get(0);
        assertThat(item.title()).isEqualTo("보고서");
        assertThat(item.personalTags()).containsExactlyInAnyOrder("업무", "중요");
    }

    @Test
    @DisplayName("추천으로 수락된 일정은 isRecommended=true")
    void isRecommendedMapsFromAcceptedRecommendation() {
        Schedule fromRec = pin("추천에서 온 일정", LocalDate.of(2026, 6, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("직접 만든 일정", LocalDate.of(2026, 6, 2), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        recommendationRepository.save(Recommendation.builder()
                .memberId(MEMBER_ID).date(LocalDate.of(2026, 6, 1))
                .conditionTag(ConditionTag.CORE_TASK)
                .sourceType(RecommendationSourceType.QUEUE_CARD)
                .acceptedScheduleId(fromRec.getScheduleId())
                .build());

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID, empty(), 0);

        assertThat(res.data()).filteredOn(ScheduleSearchResponse::isRecommended)
                .extracting(ScheduleSearchResponse::title).containsExactly("추천에서 온 일정");
    }

    @Test
    @DisplayName("페이지네이션 — 페이지당 30개, 날짜 오름차순")
    void paginationThirtyPerPage() {
        for (int i = 1; i <= 35; i++) {
            pin("카드" + i, LocalDate.of(2026, 6, 1).plusDays(i), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        }

        PageResponse<ScheduleSearchResponse> p0 = searchService.search(MEMBER_ID, empty(), 0);
        PageResponse<ScheduleSearchResponse> p1 = searchService.search(MEMBER_ID, empty(), 1);

        assertThat(p0.data()).hasSize(30);
        assertThat(p0.data().get(0).title()).isEqualTo("카드1");   // 가장 이른 날짜
        assertThat(p0.pagination().totalElements()).isEqualTo(35);
        assertThat(p0.pagination().hasNext()).isTrue();
        assertThat(p1.data()).hasSize(5);
        assertThat(p1.data().get(4).title()).isEqualTo("카드35");  // 가장 늦은 날짜
        assertThat(p1.pagination().hasNext()).isFalse();
    }

    @Test
    @DisplayName("응답 항목이 스케줄 도메인 컨벤션(snake_case, 시간·날짜=String)으로 500 없이 직렬화된다")
    void responseSerializesWithConventionKeys() throws Exception {
        ScheduleSearchResponse sample = ScheduleSearchResponse.of(
                pin("보고서", LocalDate.of(2026, 6, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK),
                List.of("업무"), true);

        String json = objectMapper.writeValueAsString(sample);

        assertThat(json)
                .contains("\"schedule_id\":")
                .contains("\"date\":\"2026-06-01\"")
                .contains("\"start_time\":\"10:00\"")
                .contains("\"condition_tag\":\"CORE_TASK\"")
                .contains("\"personal_tags\":[\"업무\"]")
                .contains("\"estimated_time\":")
                .contains("\"is_recommended\":true")
                .contains("\"is_conflict\":false");
    }

    // ─────────────────────────── 헬퍼 ───────────────────────────

    private Schedule pin(String title, LocalDate date, ScheduleStatus status, ConditionTag tag) {
        return pin(title, date, status, tag, MEMBER_ID);
    }

    private Schedule pin(String title, LocalDate date, ScheduleStatus status, ConditionTag tag, Long memberId) {
        return scheduleRepository.save(Schedule.builder()
                .memberId(memberId).title(title).conditionTag(tag)
                .date(date).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .isQueue(false).isRecurring(false).isConflict(false)
                .status(status).build());
    }

    private Schedule queue(String title, LocalDate date, ScheduleStatus status, ConditionTag tag) {
        return scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID).title(title).conditionTag(tag)
                .date(date).estimatedTime(30)
                .isQueue(true).isRecurring(false).isConflict(false)
                .status(status).build());
    }
}
