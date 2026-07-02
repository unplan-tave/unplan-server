package com.unplan.unplanserver.domain.recommendation.service;

import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse;
import com.unplan.unplanserver.domain.measurement.service.MeasurementService;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse;
import com.unplan.unplanserver.domain.recommendation.engine.EmptyTimeFinder;
import com.unplan.unplanserver.domain.recommendation.engine.RecommendationMatcher;
import com.unplan.unplanserver.domain.recommendation.entity.Recommendation;
import com.unplan.unplanserver.domain.recommendation.enums.RecommendationStatus;
import com.unplan.unplanserver.domain.recommendation.repository.RecommendationRepository;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 추천 생성 서비스(빈 시간 탐색 → 필터 → 매칭 → 정렬 → 슬롯 배치 → 영속화) 조립 검증.
 * 엔진(EmptyTimeFinder/RecommendationMatcher)은 실물, 데이터 소스는 목으로 대체.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.parse("2026-07-03");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 3, 14, 0);

    @Mock private ScheduleService scheduleService;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private RecommendationRepository recommendationRepository;
    @Mock private MeasurementService measurementService;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationService(scheduleService, scheduleRepository, recommendationRepository,
                measurementService, new EmptyTimeFinder(), new RecommendationMatcher());
    }

    // ─────────────────────────── 헬퍼 ───────────────────────────

    private void givenConditionTag(String label) {
        when(measurementService.getDailyRecord(eq(MEMBER_ID), eq(TODAY)))
                .thenReturn(new MeasurementRecordResponse(TODAY, 80, "집중 가능", label,
                        80, 80, 80, 420, List.of(), List.of()));
    }

    private void givenNoRejected() {
        when(recommendationRepository.findByMemberIdAndDateAndStatusIn(eq(MEMBER_ID), any(), any()))
                .thenReturn(List.of());
    }

    private void givenSaveReturnsArgument() {
        when(recommendationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Schedule pin(String start, String end) {
        return Schedule.builder()
                .scheduleId(99L).memberId(MEMBER_ID).title("핀")
                .date(TODAY).startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end))
                .isQueue(false).status(ScheduleStatus.TODO)
                .build();
    }

    private Schedule queue(long id, String title, ConditionTag tag, Integer estimated, LocalDate deadline) {
        return Schedule.builder()
                .scheduleId(id).memberId(MEMBER_ID).title(title)
                .conditionTag(tag).estimatedTime(estimated).date(deadline)
                .isQueue(true).status(ScheduleStatus.TODO)
                .createdAt(LocalDateTime.of(2026, 6, 1, 0, 0).plusMinutes(id))
                .build();
    }

    // ─────────────────────────── 시나리오 ───────────────────────────

    @Test
    @DisplayName("핀 카드 버퍼를 제외한 첫 빈 시간에, 소요시간이 맞고 태그가 정확 일치하는 카드를 배치한다")
    void basicRecommendation() {
        givenConditionTag("핵심 작업");
        givenNoRejected();
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of(pin("15:00", "16:00")));
        when(scheduleRepository.findByMemberIdAndIsQueueTrue(MEMBER_ID)).thenReturn(List.of(
                queue(11L, "과제", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-04")),
                queue(13L, "독서", ConditionTag.BRAIN_WORK, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        // 빈 시간: 14:00~14:45 (핀 15:00 - 버퍼 15분)
        assertThat(res.conditionTag()).isEqualTo("CORE_TASK");
        assertThat(res.emptyTime().startTime()).isEqualTo(LocalTime.parse("14:00"));
        assertThat(res.emptyTime().endTime()).isEqualTo(LocalTime.parse("14:45"));
        assertThat(res.emptyTime().durationMinutes()).isEqualTo(45);
        // 정확 일치(핵심 작업) 카드만 — 인접 태그(두뇌 활동)는 1순위가 있으므로 제외
        assertThat(res.recommendations()).hasSize(1);
        RecommendationListResponse.RecommendationItem item = res.recommendations().get(0);
        assertThat(item.title()).isEqualTo("과제");
        assertThat(item.startTime()).isEqualTo(LocalTime.parse("14:00"));
        assertThat(item.endTime()).isEqualTo(LocalTime.parse("14:30"));
        assertThat(item.deadline()).isEqualTo(LocalDate.parse("2026-07-04"));
        assertThat(item.displayOrder()).isZero();
        // 재생성: 이전 PENDING 정리
        verify(recommendationRepository).deleteByMemberIdAndDateAndStatus(MEMBER_ID, TODAY, RecommendationStatus.PENDING);
    }

    @Test
    @DisplayName("첫 빈 시간에 안 들어가는 카드는 다음 빈 시간(자정까지)에 배치된다")
    void cardMovesToNextSlot() {
        givenConditionTag("핵심 작업");
        givenNoRejected();
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of(pin("15:00", "16:00")));
        when(scheduleRepository.findByMemberIdAndIsQueueTrue(MEMBER_ID)).thenReturn(List.of(
                queue(11L, "보고서", ConditionTag.CORE_TASK, 60, null))); // 45분 슬롯엔 안 맞음

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        // 두 번째 빈 시간: 16:15 ~ 자정(00:00 규약)
        assertThat(res.emptyTime().startTime()).isEqualTo(LocalTime.parse("16:15"));
        assertThat(res.emptyTime().endTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(res.emptyTime().durationMinutes()).isEqualTo(465);
        assertThat(res.recommendations()).hasSize(1);
        assertThat(res.recommendations().get(0).startTime()).isEqualTo(LocalTime.parse("16:15"));
        assertThat(res.recommendations().get(0).endTime()).isEqualTo(LocalTime.parse("17:15"));
    }

    @Test
    @DisplayName("같은 날짜에 거절(REJECTED)된 원본 큐 카드는 재계산에서 제외된다")
    void rejectedSourceExcluded() {
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        when(recommendationRepository.findByMemberIdAndDateAndStatusIn(eq(MEMBER_ID), any(), any()))
                .thenReturn(List.of(Recommendation.builder().sourceScheduleId(11L).build()));
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findByMemberIdAndIsQueueTrue(MEMBER_ID)).thenReturn(List.of(
                queue(11L, "거절된 카드", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-04")),
                queue(14L, "남은 카드", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-05"))));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.recommendations()).hasSize(1);
        assertThat(res.recommendations().get(0).title()).isEqualTo("남은 카드");
    }

    @Test
    @DisplayName("기력 회복 상태에서는 기력 회복 태그 카드만 추천된다 (회복 수단은 기획 확정 대기)")
    void recoveryStateOnlyRecoveryCards() {
        givenConditionTag("기력 회복");
        givenNoRejected();
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findByMemberIdAndIsQueueTrue(MEMBER_ID)).thenReturn(List.of(
                queue(21L, "낮잠", ConditionTag.RECOVERY, 30, null),
                queue(22L, "메일 정리", ConditionTag.DAILY_TASK, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.conditionTag()).isEqualTo("RECOVERY");
        assertThat(res.recommendations()).hasSize(1);
        assertThat(res.recommendations().get(0).title()).isEqualTo("낮잠");
    }

    @Test
    @DisplayName("과거 날짜는 추천을 생성하지 않고 빈 응답")
    void pastDateReturnsEmpty() {
        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY.minusDays(1), NOW);

        assertThat(res.recommendations()).isEmpty();
        assertThat(res.emptyTime()).isNull();
        verifyNoInteractions(measurementService, scheduleRepository, recommendationRepository);
    }

    @Test
    @DisplayName("소요시간 미정 카드는 후보에서 제외, 완료 카드도 제외")
    void undefinedEstimateAndDoneExcluded() {
        givenConditionTag("핵심 작업");
        givenNoRejected();
        givenSaveReturnsArgument();
        Schedule done = Schedule.builder()
                .scheduleId(31L).memberId(MEMBER_ID).title("끝난 일")
                .conditionTag(ConditionTag.CORE_TASK).estimatedTime(30)
                .isQueue(true).status(ScheduleStatus.DONE)
                .createdAt(LocalDateTime.of(2026, 6, 1, 0, 0))
                .build();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findByMemberIdAndIsQueueTrue(MEMBER_ID)).thenReturn(List.of(
                queue(32L, "소요시간 미정", ConditionTag.CORE_TASK, null, null),
                done,
                queue(33L, "정상 카드", ConditionTag.CORE_TASK, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.recommendations()).hasSize(1);
        assertThat(res.recommendations().get(0).title()).isEqualTo("정상 카드");
    }

    @Test
    @DisplayName("매칭 후보가 하나도 없으면 빈 목록 + 현재 태그는 유지")
    void noCandidatesReturnsEmptyList() {
        givenConditionTag("핵심 작업");
        givenNoRejected();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findByMemberIdAndIsQueueTrue(MEMBER_ID)).thenReturn(List.of());

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.conditionTag()).isEqualTo("CORE_TASK");
        assertThat(res.emptyTime()).isNull();
        assertThat(res.recommendations()).isEmpty();
        // 이전 노출분은 그래도 정리되어야 함
        verify(recommendationRepository).deleteByMemberIdAndDateAndStatus(MEMBER_ID, TODAY, RecommendationStatus.PENDING);
    }

    @Test
    @DisplayName("알 수 없는 컨디션 태그 라벨은 500 대신 일상 작업으로 폴백한다")
    void unknownLabelFallsBackToDailyTask() {
        givenConditionTag("존재하지 않는 라벨");
        givenNoRejected();
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findByMemberIdAndIsQueueTrue(MEMBER_ID)).thenReturn(List.of(
                queue(41L, "루틴 업무", ConditionTag.DAILY_TASK, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.conditionTag()).isEqualTo("DAILY_TASK");
        assertThat(res.recommendations()).hasSize(1);
    }

    @Test
    @DisplayName("마감 임박순 정렬 + 최대 4개 제한이 적용된다")
    void sortedByDeadlineAndCapped() {
        givenConditionTag("핵심 작업");
        givenNoRejected();
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findByMemberIdAndIsQueueTrue(MEMBER_ID)).thenReturn(List.of(
                queue(51L, "마감 셋째", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-07")),
                queue(52L, "마감 첫째", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-04")),
                queue(53L, "마감 없음", ConditionTag.CORE_TASK, 30, null),
                queue(54L, "마감 둘째", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-05")),
                queue(55L, "마감 넷째", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-08"))));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.recommendations()).hasSize(4); // MAX_RECOMMENDATIONS
        assertThat(res.recommendations()).extracting(RecommendationListResponse.RecommendationItem::title)
                .containsExactly("마감 첫째", "마감 둘째", "마감 셋째", "마감 넷째"); // 마감 없음은 5순위로 잘림
        assertThat(res.recommendations()).extracting(RecommendationListResponse.RecommendationItem::displayOrder)
                .containsExactly(0, 1, 2, 3);
    }
}
