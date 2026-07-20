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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
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
    // 기간필터 미전송 시 기본 범위(오늘 ±3개월) 안에 항상 들도록, 무필터 검색 테스트는 오늘 기준 상대 날짜를 쓴다.
    // 서비스가 Asia/Seoul 기준으로 today 를 계산하므로 테스트도 동일 존을 써야 자정 전후 CI(UTC)에서 경계 테스트가 흔들리지 않는다.
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Seoul"));

    private ScheduleSearchCondition cond(String keyword, Boolean isQueue,
                                         List<ScheduleStatus> statuses, List<ConditionTag> tags, List<String> personalTags) {
        return new ScheduleSearchCondition(keyword, isQueue, statuses, tags, personalTags, null, null);
    }

    private ScheduleSearchCondition dateTimeRange(LocalDateTime start, LocalDateTime end) {
        return new ScheduleSearchCondition(null, null, null, null, null, start, end);
    }

    private ScheduleSearchCondition empty() {
        return cond(null, null, null, null, null);
    }

    @Test
    @DisplayName("필터 없으면 본인 일정만 최신순(날짜 내림차순)으로 반환한다")
    void noFilterReturnsOwnSortedByDate() {
        pin("회의", TODAY.plusDays(2), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("운동", TODAY, ScheduleStatus.DONE, ConditionTag.RECOVERY);
        queue("과제", TODAY.plusDays(1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("남의 일정", TODAY, ScheduleStatus.TODO, ConditionTag.CORE_TASK, OTHER_MEMBER);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID, empty(), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title)
                .containsExactly("회의", "과제", "운동"); // +2, +1, 오늘 (최신순) — 남의 것 제외
        assertThat(res.pagination().totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("keyword 는 제목 부분일치(대소문자 무시)")
    void keywordFiltersByTitle() {
        pin("Team Meeting", TODAY, ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("점심 약속", TODAY.plusDays(1), ScheduleStatus.TODO, ConditionTag.DAILY_TASK);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID, cond("meeting", null, null, null, null), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title).containsExactly("Team Meeting");
    }

    @Test
    @DisplayName("isQueue·status·conditionTag 필터가 AND 로 좁힌다")
    void combinedFiltersAnd() {
        queue("큐-할일-코어", TODAY, ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        queue("큐-완료-코어", TODAY.plusDays(1), ScheduleStatus.DONE, ConditionTag.CORE_TASK);
        pin("핀-할일-코어", TODAY.plusDays(2), ScheduleStatus.TODO, ConditionTag.CORE_TASK);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID,
                cond(null, true, List.of(ScheduleStatus.TODO), List.of(ConditionTag.CORE_TASK), null), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title).containsExactly("큐-할일-코어");
    }

    @Test
    @DisplayName("status 복수 지정은 OR 로 매칭한다")
    void statusMultiOr() {
        pin("할일", TODAY, ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("진행중", TODAY.plusDays(1), ScheduleStatus.IN_PROGRESS, ConditionTag.CORE_TASK);
        pin("완료", TODAY.plusDays(2), ScheduleStatus.DONE, ConditionTag.CORE_TASK);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID,
                cond(null, null, List.of(ScheduleStatus.TODO, ScheduleStatus.IN_PROGRESS), null, null), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title).containsExactly("진행중", "할일");
    }

    @Test
    @DisplayName("datetime 구간 필터는 카드 시간구간이 겹치는 핀 카드만 매칭하고 큐 카드는 제외한다")
    void dateTimeRangeFilterOverlapExcludesQueue() {
        LocalDate a = LocalDate.of(2026, 6, 28);
        LocalDate b = LocalDate.of(2026, 6, 29);
        pinOn("A-이른", a, LocalTime.of(9, 0), LocalTime.of(10, 0));    // 시작일, 필터시작(14:30) 전에 끝남 → 제외
        pinOn("A-겹침", a, LocalTime.of(14, 0), LocalTime.of(15, 0));   // 시작일, 14:30 이후까지 지속 → 매칭
        pinOn("A-저녁", a, LocalTime.of(20, 0), LocalTime.of(21, 0));   // 시작일 이후 시간대 → 매칭
        pinOn("B-아침", b, LocalTime.of(8, 0), LocalTime.of(9, 0));     // 종료일, 필터끝(12:00) 전에 시작 → 매칭
        pinOn("B-오후", b, LocalTime.of(13, 0), LocalTime.of(14, 0));   // 종료일, 12:00 이후 시작 → 제외
        queue("큐-시간없음", a, ScheduleStatus.TODO, ConditionTag.CORE_TASK); // 시간 없음 → 제외

        // 필터 구간 [6/28 14:30, 6/29 12:00]
        var res = searchService.search(MEMBER_ID,
                dateTimeRange(a.atTime(14, 30), b.atTime(12, 0)), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title)
                .containsExactlyInAnyOrder("A-겹침", "A-저녁", "B-아침");
    }

    @Test
    @DisplayName("datetime 검색 범위와 날짜 범위가 겹치는 기간 핀 카드를 포함한다")
    void dateTimeRangeIncludesOverlappingPeriodPin() {
        LocalDate start = LocalDate.of(2026, 7, 20);
        Schedule period = Schedule.builder()
                .memberId(MEMBER_ID).title("3일 일정").conditionTag(ConditionTag.CORE_TASK)
                .date(start).endDate(start.plusDays(2))
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .isQueue(false).isRecurring(false).isConflict(false)
                .status(ScheduleStatus.TODO).build();
        scheduleRepository.save(period);

        var middleDay = searchService.search(MEMBER_ID,
                dateTimeRange(start.plusDays(1).atStartOfDay(), start.plusDays(1).atTime(23, 59)), 0);
        var afterPeriod = searchService.search(MEMBER_ID,
                dateTimeRange(start.plusDays(3).atStartOfDay(), start.plusDays(3).atTime(23, 59)), 0);

        assertThat(middleDay.data()).extracting(ScheduleSearchResponse::title).containsExactly("3일 일정");
        assertThat(afterPeriod.data()).extracting(ScheduleSearchResponse::title).doesNotContain("3일 일정");
    }

    @Test
    @DisplayName("기간필터 미전송 시 오늘 기준 앞뒤 3개월만 반환하고 범위 밖은 제외한다")
    void noDateFilterDefaultsToPlusMinusThreeMonths() {
        pin("범위전", TODAY.minusMonths(4), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("하한경계", TODAY.minusMonths(3), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("오늘", TODAY, ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("상한경계", TODAY.plusMonths(3), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("범위후", TODAY.plusMonths(4), ScheduleStatus.TODO, ConditionTag.CORE_TASK);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID, empty(), 0);

        // 하한·상한 경계 포함(inclusive), 범위 밖 ±4개월은 제외 — 최신순(날짜 DESC)
        assertThat(res.data()).extracting(ScheduleSearchResponse::title)
                .containsExactly("상한경계", "오늘", "하한경계");
        assertThat(res.pagination().totalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("기간필터를 명시하면 기본 ±3개월 범위를 넘어서도 그대로 조회된다")
    void explicitDateRangeOverridesDefault() {
        pin("반년전", TODAY.minusMonths(6), ScheduleStatus.TODO, ConditionTag.CORE_TASK);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID,
                dateTimeRange(TODAY.minusMonths(7).atStartOfDay(), TODAY.minusMonths(5).atStartOfDay()), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title).containsExactly("반년전");
    }

    @Test
    @DisplayName("무필터 기본 ±3개월 범위에는 시간 없는 큐 카드도 포함된다")
    void defaultRangeIncludesQueueCards() {
        queue("큐-이번달", TODAY, ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("핀-이번달", TODAY, ScheduleStatus.TODO, ConditionTag.CORE_TASK);

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID, empty(), 0);

        assertThat(res.data()).extracting(ScheduleSearchResponse::title)
                .contains("큐-이번달", "핀-이번달");
    }

    @Test
    @DisplayName("datetime 기간 필터는 양끝 포함에 가깝게 겹치는 핀 카드를 조회하고, 한쪽만 주면 그 방향만 제한한다")
    void dateRangeFilterInclusiveAndOpenEnded() {
        pin("5월", LocalDate.of(2026, 5, 31), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("6월시작", LocalDate.of(2026, 6, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("6월중순", LocalDate.of(2026, 6, 15), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("6월끝", LocalDate.of(2026, 6, 30), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("7월", LocalDate.of(2026, 7, 1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
    
        // [6/1 00:00, 6/30 23:59]와 겹치는 핀 카드
        assertThat(searchService.search(MEMBER_ID,
                dateTimeRange(
                        LocalDateTime.of(2026, 6, 1, 0, 0),
                        LocalDateTime.of(2026, 6, 30, 23, 59)
                ), 0).data())
                .extracting(ScheduleSearchResponse::title)
                .containsExactly("6월끝", "6월중순", "6월시작");
    
        // startDateTime 만 → 그 시각 이후까지 지속되는 카드
        assertThat(searchService.search(MEMBER_ID,
                dateTimeRange(
                        LocalDateTime.of(2026, 6, 30, 0, 0),
                        null
                ), 0).data())
                .extracting(ScheduleSearchResponse::title)
                .containsExactly("7월", "6월끝");
    
        // endDateTime 만 → 그 시각 이전에 시작한 카드
        assertThat(searchService.search(MEMBER_ID,
                dateTimeRange(
                        null,
                        LocalDateTime.of(2026, 6, 1, 23, 59)
                ), 0).data())
                .extracting(ScheduleSearchResponse::title)
                .containsExactly("6월시작", "5월");
    }

    @Test
    @DisplayName("personalTags 는 하나라도 연결된 일정을 매칭하고, 응답에 태그가 담긴다")
    void personalTagsOrAndAppearInResponse() {
        Schedule a = pin("보고서", TODAY, ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        tagService.attachTags(a, MEMBER_ID, List.of("업무", "중요"));
        Schedule b = pin("산책", TODAY.plusDays(1), ScheduleStatus.TODO, ConditionTag.RECOVERY);
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
        Schedule fromRec = pin("추천에서 온 일정", TODAY, ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        pin("직접 만든 일정", TODAY.plusDays(1), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        recommendationRepository.save(Recommendation.builder()
                .memberId(MEMBER_ID).date(TODAY)
                .conditionTag(ConditionTag.CORE_TASK)
                .sourceType(RecommendationSourceType.QUEUE_CARD)
                .acceptedScheduleId(fromRec.getScheduleId())
                .build());

        PageResponse<ScheduleSearchResponse> res = searchService.search(MEMBER_ID, empty(), 0);

        assertThat(res.data()).filteredOn(ScheduleSearchResponse::isRecommended)
                .extracting(ScheduleSearchResponse::title).containsExactly("추천에서 온 일정");
    }

    @Test
    @DisplayName("페이지네이션 — 페이지당 30개, 최신순(날짜 내림차순)")
    void paginationThirtyPerPage() {
        for (int i = 1; i <= 35; i++) {
            // 오늘 ±3개월 기본 범위 안에 들도록 오늘 기준 미래로 배치 (35일 < 3개월)
            pin("카드" + i, TODAY.plusDays(i), ScheduleStatus.TODO, ConditionTag.CORE_TASK);
        }

        PageResponse<ScheduleSearchResponse> p0 = searchService.search(MEMBER_ID, empty(), 0);
        PageResponse<ScheduleSearchResponse> p1 = searchService.search(MEMBER_ID, empty(), 1);

        assertThat(p0.data()).hasSize(30);
        assertThat(p0.data().get(0).title()).isEqualTo("카드35");  // 가장 늦은 날짜(최신순 첫번째)
        assertThat(p0.pagination().totalElements()).isEqualTo(35);
        assertThat(p0.pagination().hasNext()).isTrue();
        assertThat(p1.data()).hasSize(5);
        assertThat(p1.data().get(4).title()).isEqualTo("카드1");   // 가장 이른 날짜(최신순 마지막)
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

    private Schedule pinOn(String title, LocalDate date, LocalTime start, LocalTime end) {
        return scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID).title(title).conditionTag(ConditionTag.CORE_TASK)
                .date(date).startTime(start).endTime(end)
                .isQueue(false).isRecurring(false).isConflict(false)
                .status(ScheduleStatus.TODO).build());
    }

    private Schedule queue(String title, LocalDate date, ScheduleStatus status, ConditionTag tag) {
        return scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID).title(title).conditionTag(tag)
                .date(date).estimatedTime(30)
                .isQueue(true).isRecurring(false).isConflict(false)
                .status(status).build());
    }
}
