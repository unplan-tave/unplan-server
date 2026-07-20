package com.unplan.unplanserver.domain.recommendation.service;

import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse;
import com.unplan.unplanserver.domain.measurement.service.MeasurementService;
import com.unplan.unplanserver.domain.onboarding.entity.Biorhythm;
import com.unplan.unplanserver.domain.onboarding.repository.BiorhythmRepository;
import com.unplan.unplanserver.domain.onboarding.service.RecoverService;
import com.unplan.unplanserver.domain.recommendation.dto.response.ConditionRecommendationResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.QueueCardRecommendationResult;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationAcceptResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse;
import com.unplan.unplanserver.domain.recommendation.engine.EmptyTimeFinder;
import com.unplan.unplanserver.domain.recommendation.engine.RecommendationMatcher;
import com.unplan.unplanserver.domain.recommendation.entity.Recommendation;
import com.unplan.unplanserver.domain.recommendation.enums.RecommendationSourceType;
import com.unplan.unplanserver.domain.recommendation.repository.RecommendationRepository;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import com.unplan.unplanserver.domain.schedule.service.TagService;
import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingRequestDto;
import com.unplan.unplanserver.domain.setting.entity.Setting;
import com.unplan.unplanserver.domain.setting.repository.SettingRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import com.unplan.unplanserver.global.response.PageResponse;
import com.unplan.unplanserver.global.response.PageResponse.PaginationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
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
    @Mock private TagService tagService;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private RecommendationRepository recommendationRepository;
    @Mock private MeasurementService measurementService;
    @Mock private RecoverService recoverService;
    @Mock private BiorhythmRepository biorhythmRepository;
    @Mock private SettingRepository settingRepository;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        // settingRepository 는 기본이 Optional.empty() = 설정 없는 회원(제약 없음). 설정 반영 테스트에서만 스텁.
        service = new RecommendationService(scheduleService, tagService, scheduleRepository, recommendationRepository,
                measurementService, recoverService, biorhythmRepository, settingRepository,
                new EmptyTimeFinder(), new RecommendationMatcher());
    }

    // ─────────────────────────── 헬퍼 ───────────────────────────

    private void givenConditionTag(String label) {
        when(measurementService.getDailyRecord(eq(MEMBER_ID), eq(TODAY)))
                .thenReturn(new MeasurementRecordResponse(TODAY, 80, "집중 가능", label,
                        80, 80, 80, 420, List.of(), List.of(), true, true));
    }

    private static <T> PageResponse<T> emptyPage() {
        return new PageResponse<>(
                List.of(),
                new PaginationInfo(0, 30, 0, 0, false, false)
        );
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
    @DisplayName("첫 빈 시간에, 정확 일치 카드를 먼저 두고 부족분은 인접 태그로 이어 채운다")
    void basicRecommendation() {
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of(pin("15:00", "16:00")));
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(11L, "과제", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-04")),
                queue(13L, "독서", ConditionTag.BRAIN_WORK, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        // 빈 시간: 14:00~14:45 (핀 15:00 - 버퍼 15분)
        assertThat(res.conditionTag()).isEqualTo("CORE_TASK");
        assertThat(res.emptyTime().startTime()).isEqualTo(LocalTime.parse("14:00"));
        assertThat(res.emptyTime().endTime()).isEqualTo(LocalTime.parse("14:45"));
        assertThat(res.emptyTime().durationMinutes()).isEqualTo(45);
        // 교차 채우기: 정확 일치(과제) 먼저 + 인접(독서)로 이어 채움 → 2개 (둘 다 45분 슬롯에 맞음)
        assertThat(res.recommendations()).hasSize(2);
        RecommendationListResponse.RecommendationItem item = res.recommendations().get(0);
        assertThat(item.title()).isEqualTo("과제");
        assertThat(item.startTime()).isEqualTo(LocalTime.parse("14:00"));
        assertThat(item.endTime()).isEqualTo(LocalTime.parse("14:30"));
        assertThat(item.deadline()).isEqualTo(LocalDate.parse("2026-07-04"));
        assertThat(item.displayOrder()).isZero();
        assertThat(item.matchTier()).isEqualTo("EXACT"); // 적합도 멘트용 매칭 순위 노출
        assertThat(res.recommendations().get(1).title()).isEqualTo("독서");
        assertThat(res.recommendations().get(1).displayOrder()).isEqualTo(1);
        assertThat(res.recommendations().get(1).matchTier()).isEqualTo("ADJACENT");
        // 재생성: 이전 PENDING 정리
        verify(recommendationRepository).deleteByMemberIdAndDateAndAcceptedScheduleIdIsNull(MEMBER_ID, TODAY);
    }

    @Test
    @DisplayName("첫 빈 시간에 안 들어가는 카드는 다음 빈 시간(자정까지)에 배치된다")
    void cardMovesToNextSlot() {
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of(pin("15:00", "16:00")));
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
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
    @DisplayName("설정(#83)의 '최소 여유 시간' 미만 슬롯은 건너뛴다")
    void minGapSettingSkipsShortSlots() {
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        Setting setting = new Setting(MEMBER_ID);
        setting.update(new EmptyTimeSettingRequestDto(true, 60, false, null)); // 최소 60분, 제외 시간대 off
        when(settingRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(setting));
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of(pin("15:00", "16:00")));
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(11L, "과제", ConditionTag.CORE_TASK, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        // 14:00~14:45(45분)는 최소 여유 시간 60분 미만이라 탈락 → 16:15~자정 슬롯에 배치
        assertThat(res.emptyTime().startTime()).isEqualTo(LocalTime.parse("16:15"));
        assertThat(res.recommendations().get(0).startTime()).isEqualTo(LocalTime.parse("16:15"));
    }

    @Test
    @DisplayName("설정(#83)의 '추천 제외 시간대'는 버퍼 없이 그대로 차단된다")
    void banTimeSettingBlocksWithoutBuffer() {
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        Setting setting = new Setting(MEMBER_ID);
        setting.update(new EmptyTimeSettingRequestDto(false, null, true,
                List.of(new EmptyTimeSettingRequestDto.RecommendBanTime(
                        LocalTime.parse("18:00"), LocalTime.parse("23:59")))));
        when(settingRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(setting));
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(11L, "과제", ConditionTag.CORE_TASK, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        // 빈 시간: 14:00~18:00. 제외 시간대에 핀 카드용 15분 버퍼가 붙으면 17:45 로 끝나므로 18:00 확인이 핵심
        assertThat(res.emptyTime().startTime()).isEqualTo(LocalTime.parse("14:00"));
        assertThat(res.emptyTime().endTime()).isEqualTo(LocalTime.parse("18:00"));
    }

    @Test
    @DisplayName("기력 회복 상태: 기력회복 카드 뒤에 '회복 수단' 후보 1건을 붙여 노출한다")
    void recoveryAppendsRecoveryMeanCandidate() {
        givenConditionTag("기력 회복");
        givenSaveReturnsArgument();
        when(recoverService.getRecoveryMeanLabels(MEMBER_ID)).thenReturn(List.of("짧은 낮잠", "음악 감상"));
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(21L, "낮잠 큐카드", ConditionTag.RECOVERY, 30, null),
                queue(22L, "메일 정리", ConditionTag.DAILY_TASK, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.conditionTag()).isEqualTo("RECOVERY");
        assertThat(res.recommendations()).hasSize(2);
        // 1) 기력회복 태그 큐 카드
        RecommendationListResponse.RecommendationItem card = res.recommendations().get(0);
        assertThat(card.title()).isEqualTo("낮잠 큐카드");
        assertThat(card.sourceType()).isEqualTo("QUEUE_CARD");
        assertThat(card.matchTier()).isEqualTo("EXACT"); // 회복 태그 카드는 정확 일치 취급
        assertThat(card.recoveryMeans()).isNull();
        // 2) 회복 수단 후보: 제목 미선택(null), 옵션은 설정 순서, 길이 min(빈시간,30)
        RecommendationListResponse.RecommendationItem mean = res.recommendations().get(1);
        assertThat(mean.sourceType()).isEqualTo("RECOVERY_MEAN");
        assertThat(mean.title()).isNull();
        assertThat(mean.conditionTag()).isEqualTo("RECOVERY");
        assertThat(mean.recoveryMeans()).containsExactly("짧은 낮잠", "음악 감상");
        assertThat(mean.estimatedTime()).isEqualTo(30);
        assertThat(mean.displayOrder()).isEqualTo(1);
        assertThat(mean.matchTier()).isNull(); // 회복 수단 후보는 태그 매칭 결과가 아님
    }

    @Test
    @DisplayName("기력 회복인데 기력회복 카드가 없으면 회복 수단 후보만 단독 노출")
    void recoveryMeanOnlyWhenNoRecoveryCards() {
        givenConditionTag("기력 회복");
        givenSaveReturnsArgument();
        when(recoverService.getRecoveryMeanLabels(MEMBER_ID)).thenReturn(List.of("짧은 낮잠"));
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(31L, "메일 정리", ConditionTag.DAILY_TASK, 30, null))); // 기력회복 태그 카드 없음

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.recommendations()).hasSize(1);
        assertThat(res.recommendations().get(0).sourceType()).isEqualTo("RECOVERY_MEAN");
        assertThat(res.recommendations().get(0).recoveryMeans()).containsExactly("짧은 낮잠");
    }

    @Test
    @DisplayName("기력회복 카드가 이미 3개(MAX)면 회복 수단 후보는 붙지 않는다")
    void recoveryMeanNotAddedWhenCardsFillLimit() {
        givenConditionTag("기력 회복");
        givenSaveReturnsArgument();
        when(recoverService.getRecoveryMeanLabels(MEMBER_ID)).thenReturn(List.of("짧은 낮잠"));
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(41L, "낮잠1", ConditionTag.RECOVERY, 30, null),
                queue(42L, "낮잠2", ConditionTag.RECOVERY, 30, null),
                queue(43L, "낮잠3", ConditionTag.RECOVERY, 30, null),
                queue(44L, "낮잠4", ConditionTag.RECOVERY, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.recommendations()).hasSize(3);
        assertThat(res.recommendations()).extracting(RecommendationListResponse.RecommendationItem::sourceType)
                .containsOnly("QUEUE_CARD");
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
    @DisplayName("후보 조회는 findActiveQueueCards 결과를 그대로 신뢰한다 (완료·소요시간 미정 제외는 쿼리 책임)")
    void usesActiveQueueCardsFromRepository() {
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        // findActiveQueueCards 가 이미 DONE·소요시간 미정을 걸러 반환한다고 가정
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(33L, "정상 카드", ConditionTag.CORE_TASK, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.recommendations()).hasSize(1);
        assertThat(res.recommendations().get(0).title()).isEqualTo("정상 카드");
    }

    @Test
    @DisplayName("매칭 후보가 하나도 없으면 빈 목록 + 현재 태그는 유지")
    void noCandidatesReturnsEmptyList() {
        givenConditionTag("핵심 작업");
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of());

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.conditionTag()).isEqualTo("CORE_TASK");
        assertThat(res.emptyTime()).isNull();
        assertThat(res.recommendations()).isEmpty();
        // 이전 노출분은 그래도 정리되어야 함
        verify(recommendationRepository).deleteByMemberIdAndDateAndAcceptedScheduleIdIsNull(MEMBER_ID, TODAY);
    }

    @Test
    @DisplayName("알 수 없는 컨디션 태그 라벨은 500 대신 일상 작업으로 폴백한다")
    void unknownLabelFallsBackToDailyTask() {
        givenConditionTag("존재하지 않는 라벨");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(41L, "루틴 업무", ConditionTag.DAILY_TASK, 30, null)));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.conditionTag()).isEqualTo("DAILY_TASK");
        assertThat(res.recommendations()).hasSize(1);
    }

    @Test
    @DisplayName("컨디션 기반 추천: 정확 일치 큐 카드와 바텀시트 문구를 반환한다")
    void conditionRecommendationExactSuccess() {
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of(pin("15:30", "16:00")));
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(61L, "몰입 과제", ConditionTag.CORE_TASK, 60, LocalDate.parse("2026-07-04"))));

        ConditionRecommendationResponse res = service.generateConditionRecommendations(MEMBER_ID, TODAY, NOW);

        assertThat(res.resultType()).isEqualTo("SUCCESS");
        assertThat(res.conditionTag()).isEqualTo("CORE_TASK");
        assertThat(res.conditionTagLabel()).isEqualTo("핵심 작업");
        assertThat(res.emptyTime().startTime()).isEqualTo(LocalTime.parse("14:00"));
        assertThat(res.emptyTime().endTime()).isEqualTo(LocalTime.parse("15:15"));
        assertThat(res.summaryMessage()).contains("14:00 ~ 15:15까지");
        assertThat(res.summaryMessage()).contains("\n");
        assertThat(res.summaryMessage()).doesNotContain("을/를");
        assertThat(res.summaryMessage()).contains("핵심 작업에 좋은 컨디션이에요");
        assertThat(res.summaryTags()).containsExactly(new ConditionRecommendationResponse.SummaryTag("CORE_TASK", "핵심 작업"));
        assertThat(res.recommendations()).hasSize(1);
        ConditionRecommendationResponse.RecommendationItem item = res.recommendations().get(0);
        assertThat(item.sourceScheduleId()).isEqualTo(61L);
        assertThat(item.conditionTagLabel()).isEqualTo("핵심 작업");
        assertThat(item.matchTier()).isEqualTo("EXACT");
        assertThat(item.suitabilityMessage()).isEqualTo("깊게 몰입하기 좋은 컨디션이에요");
        assertThat(item.timeMarginMessage()).isEqualTo("일정을 끝낸 후 약간 쉴 여유가 있어요");
    }

    @Test
    @DisplayName("컨디션 기반 추천: 오늘은 현재 시각 이전에 시작하지 않는다")
    void conditionRecommendationForTodayStartsAtOrAfterNow() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 3, 14, 30);
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(61L, "몰입 과제", ConditionTag.CORE_TASK, 60, null)));

        ConditionRecommendationResponse response =
                service.generateConditionRecommendations(MEMBER_ID, TODAY, now);

        assertThat(response.emptyTime().startTime()).isAfterOrEqualTo(now.toLocalTime());
        assertThat(response.recommendations())
                .allSatisfy(item -> assertThat(item.startTime()).isAfterOrEqualTo(now.toLocalTime()));
    }

    @Test
    @DisplayName("컨디션 기반 추천: 미래 날짜는 자정부터 탐색한다")
    void conditionRecommendationForFutureDateStartsAtMidnight() {
        LocalDate futureDate = TODAY.plusDays(1);
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, futureDate)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(61L, "몰입 과제", ConditionTag.CORE_TASK, 60, null)));

        ConditionRecommendationResponse response =
                service.generateConditionRecommendations(MEMBER_ID, futureDate, NOW);

        assertThat(response.emptyTime().startTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(response.recommendations())
                .allSatisfy(item -> assertThat(item.startTime()).isEqualTo(LocalTime.MIDNIGHT));
    }

    @Test
    @DisplayName("컨디션 기반 추천: 과거 날짜는 추천을 생성하지 않는다")
    void conditionRecommendationForPastDateReturnsNoRecommendation() {
        LocalDate pastDate = TODAY.minusDays(1);
        givenConditionTag("핵심 작업");

        ConditionRecommendationResponse response =
                service.generateConditionRecommendations(MEMBER_ID, pastDate, NOW);

        assertThat(response.resultType()).isEqualTo("NO_EMPTY_TIME");
        assertThat(response.emptyTime()).isNull();
        assertThat(response.recommendations()).isEmpty();
        verify(recommendationRepository, never())
                .deleteByMemberIdAndDateAndAcceptedScheduleIdIsNull(MEMBER_ID, pastDate);
    }

    @Test
    @DisplayName("컨디션 기반 추천: 일상 작업 정확 일치 문구는 조사를 노출하지 않는다")
    void conditionRecommendationDailyTaskExactSummaryMessage() {
        givenConditionTag("일상 작업");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(62L, "일상 정리", ConditionTag.DAILY_TASK, 30, null)));

        ConditionRecommendationResponse res = service.generateConditionRecommendations(MEMBER_ID, TODAY, NOW);

        assertThat(res.resultType()).isEqualTo("SUCCESS");
        assertThat(res.summaryMessage()).contains("\n");
        assertThat(res.summaryMessage()).doesNotContain("을/를");
        assertThat(res.summaryMessage()).contains("일상 작업에 좋은 컨디션이에요");
    }

    @Test
    @DisplayName("컨디션 기반 추천: 인접 태그 매칭은 실제 추천 태그만 summaryTags에 포함한다")
    void conditionRecommendationAdjacentSummaryTags() {
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(71L, "문서 읽기", ConditionTag.BRAIN_WORK, 30, null),
                queue(72L, "정리하기", ConditionTag.SIMPLE_TASK, 30, null)));

        ConditionRecommendationResponse res = service.generateConditionRecommendations(MEMBER_ID, TODAY, NOW);

        assertThat(res.resultType()).isEqualTo("SUCCESS");
        assertThat(res.summaryTags()).containsExactly(
                new ConditionRecommendationResponse.SummaryTag("CORE_TASK", "핵심 작업"),
                new ConditionRecommendationResponse.SummaryTag("BRAIN_WORK", "두뇌 활동"),
                new ConditionRecommendationResponse.SummaryTag("SIMPLE_TASK", "단순 노동")
        );
        assertThat(res.summaryMessage()).contains("핵심 작업, 두뇌 활동, 단순 노동 모두 괜찮아요");
        assertThat(res.recommendations()).extracting(ConditionRecommendationResponse.RecommendationItem::matchTier)
                .containsOnly("ADJACENT");
        assertThat(res.recommendations().get(0).suitabilityMessage())
                .isEqualTo("부담 없이 가볍게 시작하기 좋은 상태예요");
    }

    @Test
    @DisplayName("컨디션 기반 추천: 마감 임박 폴백은 summaryTags 없이 고정 문구를 반환한다")
    void conditionRecommendationDeadlineFallback() {
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(81L, "긴급 확인", ConditionTag.URGENT, 30, LocalDate.parse("2026-07-04"))));

        ConditionRecommendationResponse res = service.generateConditionRecommendations(MEMBER_ID, TODAY, NOW);

        assertThat(res.resultType()).isEqualTo("SUCCESS");
        assertThat(res.summaryTags()).isEmpty();
        assertThat(res.summaryMessage()).contains("지금 컨디션과 별개로 마감이 임박한 일정이에요");
        assertThat(res.recommendations().get(0).matchTier()).isEqualTo("DEADLINE");
        assertThat(res.recommendations().get(0).suitabilityMessage())
                .isEqualTo("지금 컨디션과 별개로 마감이 임박한 작업이에요");
    }

    @Test
    @DisplayName("컨디션 기반 추천: 기력 회복은 회복 큐 카드 뒤에 회복 수단 후보를 반환한다")
    void conditionRecommendationRecovery() {
        givenConditionTag("기력 회복");
        givenSaveReturnsArgument();
        when(recoverService.getRecoveryMeanLabels(MEMBER_ID)).thenReturn(List.of("짧은 낮잠", "음악 감상"));
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(91L, "휴식 큐카드", ConditionTag.RECOVERY, 30, null)));

        ConditionRecommendationResponse res = service.generateConditionRecommendations(MEMBER_ID, TODAY, NOW);

        assertThat(res.resultType()).isEqualTo("SUCCESS");
        assertThat(res.summaryTags()).containsExactly(new ConditionRecommendationResponse.SummaryTag("RECOVERY", "기력 회복"));
        assertThat(res.summaryMessage()).contains("기력 회복이 필요한 컨디션이에요");
        assertThat(res.recommendations()).hasSize(2);
        assertThat(res.recommendations().get(0).sourceType()).isEqualTo("QUEUE_CARD");
        assertThat(res.recommendations().get(0).suitabilityMessage()).isEqualTo("온전한 휴식이 필요한 컨디션이에요.");
        ConditionRecommendationResponse.RecommendationItem recoveryMean = res.recommendations().get(1);
        assertThat(recoveryMean.sourceType()).isEqualTo("RECOVERY_MEAN");
        assertThat(recoveryMean.estimatedTime()).isEqualTo(30);
        assertThat(recoveryMean.recoveryMeans()).containsExactly("짧은 낮잠", "음악 감상");
        assertThat(recoveryMean.suitabilityMessage()).isEqualTo("조금 쉬는 게 더 효율적인 타이밍이에요");
    }

    @Test
    @DisplayName("컨디션 기반 추천: 빈 시간이 없으면 NO_EMPTY_TIME")
    void conditionRecommendationNoEmptyTime() {
        givenConditionTag("핵심 작업");
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of(pin("14:00", "23:59")));

        ConditionRecommendationResponse res = service.generateConditionRecommendations(MEMBER_ID, TODAY, NOW);

        assertThat(res.resultType()).isEqualTo("NO_EMPTY_TIME");
        assertThat(res.emptyTime()).isNull();
        assertThat(res.summaryMessage()).isNull();
        assertThat(res.summaryTags()).isEmpty();
        assertThat(res.recommendations()).isEmpty();
    }

    @Test
    @DisplayName("컨디션 기반 추천: 빈 시간은 있지만 후보가 없으면 NO_MATCHING_QUEUE_CARD")
    void conditionRecommendationNoMatchingQueueCard() {
        givenConditionTag("핵심 작업");
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of());

        ConditionRecommendationResponse res = service.generateConditionRecommendations(MEMBER_ID, TODAY, NOW);

        assertThat(res.resultType()).isEqualTo("NO_MATCHING_QUEUE_CARD");
        assertThat(res.emptyTime()).isNotNull();
        assertThat(res.summaryMessage()).isNull();
        assertThat(res.summaryTags()).isEmpty();
        assertThat(res.recommendations()).isEmpty();
    }

    @Test
    @DisplayName("마감 임박순 정렬 + 최대 3개 제한이 적용된다")
    void sortedByDeadlineAndCapped() {
        givenConditionTag("핵심 작업");
        givenSaveReturnsArgument();
        when(scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY)).thenReturn(List.of());
        when(scheduleRepository.findActiveQueueCards(MEMBER_ID)).thenReturn(List.of(
                queue(51L, "마감 셋째", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-07")),
                queue(52L, "마감 첫째", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-04")),
                queue(53L, "마감 없음", ConditionTag.CORE_TASK, 30, null),
                queue(54L, "마감 둘째", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-05")),
                queue(55L, "마감 넷째", ConditionTag.CORE_TASK, 30, LocalDate.parse("2026-07-08"))));

        RecommendationListResponse res = service.generate(MEMBER_ID, TODAY, NOW);

        assertThat(res.recommendations()).hasSize(3); // MAX_RECOMMENDATIONS
        assertThat(res.recommendations()).extracting(RecommendationListResponse.RecommendationItem::title)
                .containsExactly("마감 첫째", "마감 둘째", "마감 셋째"); // 마감 넷째·마감 없음은 3개 초과로 잘림
        assertThat(res.recommendations()).extracting(RecommendationListResponse.RecommendationItem::displayOrder)
                .containsExactly(0, 1, 2);
    }

    // ─────────────────────────── 큐카드 7일 추천 ───────────────────────────

    @Test
    @DisplayName("7일 추천: 날짜별 가장 이른 빈 시간 1개씩, 오늘은 현재 시각부터 배치된다")
    void queueCardSevenDayRecommendation() {
        givenSaveReturnsArgument();
        when(scheduleRepository.findByScheduleIdAndMemberId(77L, MEMBER_ID))
                .thenReturn(Optional.of(queue(77L, "이력서 작성", ConditionTag.CORE_TASK, 30, null)));
        when(scheduleService.findSchedulesWithRecurring(eq(MEMBER_ID), any())).thenReturn(List.of());
        when(biorhythmRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());

        QueueCardRecommendationResult result = service.generateQueueCardRecommendations(MEMBER_ID, 77L, 7, NOW);

        assertThat(result.hasSlots()).isTrue();
        assertThat(result.success().scheduleId()).isEqualTo(77L);
        assertThat(result.success().title()).isEqualTo("이력서 작성");
        assertThat(result.success().estimatedTime()).isEqualTo(30);
        assertThat(result.success().rangeDays()).isEqualTo(7);
        assertThat(result.success().slots()).hasSize(7);
        // 오늘: 현재 시각(14:00)부터
        assertThat(result.success().slots().get(0).date()).isEqualTo(TODAY);
        assertThat(result.success().slots().get(0).startTime()).isEqualTo(LocalTime.parse("14:00"));
        assertThat(result.success().slots().get(0).endTime()).isEqualTo(LocalTime.parse("14:30"));
        assertThat(result.success().slots().get(0).displayOrder()).isZero();
        // 미래 날짜: 하루 시작(00:00)부터
        assertThat(result.success().slots().get(1).date()).isEqualTo(TODAY.plusDays(1));
        assertThat(result.success().slots().get(1).startTime()).isEqualTo(LocalTime.MIDNIGHT);
        assertThat(result.success().slots().get(1).displayOrder()).isEqualTo(1);
        assertThat(result.success().slots().get(6).date()).isEqualTo(TODAY.plusDays(6));
        // 재생성: 이 큐카드의 이전 노출분 정리
        verify(recommendationRepository)
                .deleteByMemberIdAndSourceScheduleIdAndAcceptedScheduleIdIsNull(MEMBER_ID, 77L);
    }

    @Test
    @DisplayName("7일 추천: 수면 시간대(sleepTimeline)에는 슬롯을 배치하지 않는다")
    void queueCardSevenDayExcludesSleep() {
        givenSaveReturnsArgument();
        when(scheduleRepository.findByScheduleIdAndMemberId(77L, MEMBER_ID))
                .thenReturn(Optional.of(queue(77L, "이력서 작성", ConditionTag.CORE_TASK, 30, null)));
        when(scheduleService.findSchedulesWithRecurring(eq(MEMBER_ID), any())).thenReturn(List.of());
        // 수면 00~06시(0~6) + 23시 → 미래 날짜의 이른 빈 시간은 수면 06:00 + 버퍼 15분 = 07:15부터
        when(biorhythmRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(
                Biorhythm.builder().memberId(MEMBER_ID)
                        .focusedTimeline("000000000000000000000000")
                        .drowsyTimeline("000000000000000000000000")
                        .sleepTimeline("111111100000000000000001")
                        .build()));

        QueueCardRecommendationResult result = service.generateQueueCardRecommendations(MEMBER_ID, 77L, 7, NOW);

        // 미래 날짜(내일): 07:15부터 (수면 07:00 종료 + 버퍼 15분)
        assertThat(result.success().slots().get(1).date()).isEqualTo(TODAY.plusDays(1));
        assertThat(result.success().slots().get(1).startTime()).isEqualTo(LocalTime.parse("07:15"));
    }

    @Test
    @DisplayName("7일 내 후보 없음: 409 canExtendTo14Days=true (소요시간 변경 아님)")
    void queueCardSevenDayNoSlotCanExtend() {
        when(scheduleRepository.findByScheduleIdAndMemberId(77L, MEMBER_ID))
                .thenReturn(Optional.of(queue(77L, "긴 일정", ConditionTag.CORE_TASK, 2000, null))); // 하루보다 김
        when(scheduleService.findSchedulesWithRecurring(eq(MEMBER_ID), any())).thenReturn(List.of());
        when(biorhythmRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());

        QueueCardRecommendationResult result = service.generateQueueCardRecommendations(MEMBER_ID, 77L, 7, NOW);

        assertThat(result.hasSlots()).isFalse();
        assertThat(result.noSlot().canExtendTo14Days()).isTrue();
        assertThat(result.noSlot().mustChangeDuration()).isFalse();
        verify(recommendationRepository, never()).save(any());
    }

    @Test
    @DisplayName("14일까지도 후보 없음: 409 mustChangeDuration=true (더 이상 확장 불가)")
    void queueCardFourteenDayNoSlotMustChangeDuration() {
        when(scheduleRepository.findByScheduleIdAndMemberId(77L, MEMBER_ID))
                .thenReturn(Optional.of(queue(77L, "긴 일정", ConditionTag.CORE_TASK, 2000, null)));
        when(scheduleService.findSchedulesWithRecurring(eq(MEMBER_ID), any())).thenReturn(List.of());
        when(biorhythmRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());

        QueueCardRecommendationResult result = service.generateQueueCardRecommendations(MEMBER_ID, 77L, 14, NOW);

        assertThat(result.hasSlots()).isFalse();
        assertThat(result.noSlot().canExtendTo14Days()).isFalse();
        assertThat(result.noSlot().mustChangeDuration()).isTrue();
    }

    @Test
    @DisplayName("소요시간 미정(estimatedTime=null) 큐카드: 즉시 409 mustChangeDuration=true, 탐색/정리 안 함")
    void queueCardWithoutEstimatedTime() {
        when(scheduleRepository.findByScheduleIdAndMemberId(77L, MEMBER_ID))
                .thenReturn(Optional.of(queue(77L, "시간 미정", ConditionTag.CORE_TASK, null, null)));

        QueueCardRecommendationResult result = service.generateQueueCardRecommendations(MEMBER_ID, 77L, 7, NOW);

        assertThat(result.hasSlots()).isFalse();
        assertThat(result.noSlot().canExtendTo14Days()).isFalse();
        assertThat(result.noSlot().mustChangeDuration()).isTrue();
        verify(recommendationRepository, never())
                .deleteByMemberIdAndSourceScheduleIdAndAcceptedScheduleIdIsNull(anyLong(), anyLong());
        verifyNoInteractions(scheduleService, biorhythmRepository);
    }

    @Test
    @DisplayName("큐카드가 아닌 일정(핀 카드)에는 NOT_A_QUEUE_CARD")
    void queueCardNotAQueue() {
        when(scheduleRepository.findByScheduleIdAndMemberId(77L, MEMBER_ID))
                .thenReturn(Optional.of(pin("14:00", "15:00"))); // isQueue=false

        assertThatThrownBy(() -> service.generateQueueCardRecommendations(MEMBER_ID, 77L, 7, NOW))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_A_QUEUE_CARD);
    }

    @Test
    @DisplayName("존재하지 않는 큐카드는 SCHEDULE_NOT_FOUND")
    void queueCardNotFound() {
        when(scheduleRepository.findByScheduleIdAndMemberId(77L, MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateQueueCardRecommendations(MEMBER_ID, 77L, 7, NOW))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.SCHEDULE_NOT_FOUND);
    }

    // ─────────────────────────── 수락 / 거절 ───────────────────────────

    private Recommendation pendingRec(RecommendationSourceType sourceType, Long sourceScheduleId,
                                      String title, String start, String end) {
        return Recommendation.builder()
                .recommendId(500L).memberId(MEMBER_ID).date(TODAY).title(title)
                .startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end))
                .conditionTag(ConditionTag.RECOVERY)
                .sourceType(sourceType).sourceScheduleId(sourceScheduleId)
                .build();
    }

    @Test
    @DisplayName("큐 카드 추천 수락은 원본 큐 카드를 핀 카드로 전환한다 (새 일정 생성 안 함)")
    void acceptQueueCardConvertsToPin() {
        Recommendation rec = pendingRec(RecommendationSourceType.QUEUE_CARD, 99L, "과제", "14:00", "14:30");
        Schedule source = queue(99L, "과제", ConditionTag.CORE_TASK, 30, TODAY.plusDays(1));
        when(recommendationRepository.findByRecommendIdAndMemberId(500L, MEMBER_ID)).thenReturn(Optional.of(rec));
        when(scheduleRepository.findByScheduleIdAndMemberId(99L, MEMBER_ID)).thenReturn(Optional.of(source));

        RecommendationAcceptResponse res = service.accept(MEMBER_ID, 500L, false, null);

        assertThat(res.created()).isFalse();
        assertThat(res.scheduleId()).isEqualTo(99L);
        assertThat(res.startTime()).isEqualTo(LocalTime.parse("14:00"));
        assertThat(res.endTime()).isEqualTo(LocalTime.parse("14:30"));
        // 원본이 핀 카드로 전환됨
        assertThat(source.getIsQueue()).isFalse();
        assertThat(source.getDate()).isEqualTo(TODAY);
        assertThat(source.getStartTime()).isEqualTo(LocalTime.parse("14:00"));
        assertThat(source.getEndTime()).isEqualTo(LocalTime.parse("14:30"));
        // 추천은 ACCEPTED, 새 일정 INSERT 없음
        assertThat(rec.getAcceptedScheduleId()).isEqualTo(99L);
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("'기존 큐 카드 유지하기' 수락은 핀 카드를 복제 생성하고 원본 큐 카드는 큐로 남긴다 (큐+핀 공존)")
    void acceptKeepQueueCardCreatesPinAndKeepsQueue() {
        Recommendation rec = pendingRec(RecommendationSourceType.QUEUE_CARD, 99L, "과제", "14:00", "14:30");
        Schedule source = queue(99L, "과제", ConditionTag.CORE_TASK, 30, TODAY.plusDays(1));
        Schedule persistedPin = Schedule.builder()
                .scheduleId(300L).memberId(MEMBER_ID).title("과제")
                .date(TODAY).startTime(LocalTime.parse("14:00")).endTime(LocalTime.parse("14:30"))
                .isQueue(false).status(ScheduleStatus.TODO).build();
        when(recommendationRepository.findByRecommendIdAndMemberId(500L, MEMBER_ID)).thenReturn(Optional.of(rec));
        when(scheduleRepository.findByScheduleIdAndMemberId(99L, MEMBER_ID)).thenReturn(Optional.of(source));
        when(scheduleRepository.save(any())).thenReturn(persistedPin);
        when(tagService.getTagNamesBySchedule(source)).thenReturn(List.of("업무", "중요"));

        RecommendationAcceptResponse res = service.accept(MEMBER_ID, 500L, true, null);

        // 새 핀 카드가 생성됨
        assertThat(res.created()).isTrue();
        assertThat(res.scheduleId()).isEqualTo(300L);
        // 원본 큐 카드는 전환되지 않고 그대로 큐로 남음 → 다음 추천 후보로 계속 노출
        assertThat(source.getIsQueue()).isTrue();
        assertThat(source.getStartTime()).isNull();
        // 저장된 핀 카드는 원본 필드를 복제하고 시간만 부여
        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        Schedule saved = captor.getValue();
        assertThat(saved.getIsQueue()).isFalse();
        assertThat(saved.getTitle()).isEqualTo("과제");
        assertThat(saved.getConditionTag()).isEqualTo(ConditionTag.CORE_TASK);
        assertThat(saved.getDate()).isEqualTo(TODAY);
        assertThat(saved.getStartTime()).isEqualTo(LocalTime.parse("14:00"));
        assertThat(saved.getEndTime()).isEqualTo(LocalTime.parse("14:30"));
        // 원본 큐 카드의 개인 태그도 복제된 핀 카드에 그대로 연결
        verify(tagService).attachTags(persistedPin, MEMBER_ID, List.of("업무", "중요"));
        // 추천은 ACCEPTED(거절 아님) → 원본 큐 카드는 재추천 후보로 유지
        assertThat(rec.getAcceptedScheduleId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("회복 수단 추천 수락은 고른 수단을 제목으로 새 일정을 생성한다")
    void acceptRecoveryMeanCreatesSchedule() {
        Recommendation rec = pendingRec(RecommendationSourceType.RECOVERY_MEAN, null, null, "14:00", "14:30");
        Schedule persisted = Schedule.builder()
                .scheduleId(200L).memberId(MEMBER_ID).title("짧은 낮잠")
                .date(TODAY).startTime(LocalTime.parse("14:00")).endTime(LocalTime.parse("14:30"))
                .isQueue(false).status(ScheduleStatus.TODO).build();
        when(recommendationRepository.findByRecommendIdAndMemberId(500L, MEMBER_ID)).thenReturn(Optional.of(rec));
        when(recoverService.getRecoveryMeanLabels(MEMBER_ID)).thenReturn(List.of("짧은 낮잠", "음악 감상"));
        when(scheduleRepository.save(any())).thenReturn(persisted);

        RecommendationAcceptResponse res = service.accept(MEMBER_ID, 500L, false, "짧은 낮잠");

        assertThat(res.created()).isTrue();
        assertThat(res.scheduleId()).isEqualTo(200L);
        assertThat(res.title()).isEqualTo("짧은 낮잠");
        // 저장된 일정 제목 = 고른 회복 수단
        ArgumentCaptor<Schedule> captor = ArgumentCaptor.forClass(Schedule.class);
        verify(scheduleRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("짧은 낮잠");
        assertThat(rec.getAcceptedScheduleId()).isEqualTo(200L);
        verify(scheduleRepository, never()).findByScheduleIdAndMemberId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("회복 수단 수락 시 고른 수단이 설정에 없으면 RECOVERY_MEAN_INVALID (일정 생성 안 함)")
    void acceptRecoveryMeanInvalid() {
        Recommendation rec = pendingRec(RecommendationSourceType.RECOVERY_MEAN, null, null, "14:00", "14:30");
        when(recommendationRepository.findByRecommendIdAndMemberId(500L, MEMBER_ID)).thenReturn(Optional.of(rec));
        when(recoverService.getRecoveryMeanLabels(MEMBER_ID)).thenReturn(List.of("짧은 낮잠"));

        assertThatThrownBy(() -> service.accept(MEMBER_ID, 500L, false, "존재하지 않는 수단"))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECOVERY_MEAN_INVALID);
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("회복 수단 수락 시 수단 미선택(null)이면 RECOVERY_MEAN_INVALID")
    void acceptRecoveryMeanMissing() {
        Recommendation rec = pendingRec(RecommendationSourceType.RECOVERY_MEAN, null, null, "14:00", "14:30");
        when(recommendationRepository.findByRecommendIdAndMemberId(500L, MEMBER_ID)).thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> service.accept(MEMBER_ID, 500L, false, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECOVERY_MEAN_INVALID);
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("슬롯 끝이 자정(00:00=24:00)인 추천을 수락하면 핀 카드 종료 시각은 23:59로 클램핑된다")
    void acceptClampsMidnightEnd() {
        Recommendation rec = pendingRec(RecommendationSourceType.QUEUE_CARD, 99L, "야간 작업", "23:30", "00:00");
        Schedule source = queue(99L, "야간 작업", ConditionTag.CORE_TASK, 30, null);
        when(recommendationRepository.findByRecommendIdAndMemberId(500L, MEMBER_ID)).thenReturn(Optional.of(rec));
        when(scheduleRepository.findByScheduleIdAndMemberId(99L, MEMBER_ID)).thenReturn(Optional.of(source));

        RecommendationAcceptResponse res = service.accept(MEMBER_ID, 500L, false, null);

        assertThat(res.endTime()).isEqualTo(LocalTime.of(23, 59));
        assertThat(source.getEndTime()).isEqualTo(LocalTime.of(23, 59));
        assertThat(source.getStartTime()).isEqualTo(LocalTime.parse("23:30"));
    }

    @Test
    @DisplayName("존재하지 않는 추천 수락은 RECOMMENDATION_NOT_FOUND")
    void acceptNotFound() {
        when(recommendationRepository.findByRecommendIdAndMemberId(500L, MEMBER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(MEMBER_ID, 500L, false, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECOMMENDATION_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 처리된(수락/거절된) 추천을 다시 수락하면 RECOMMENDATION_ALREADY_PROCESSED, 일정은 건드리지 않는다")
    void acceptAlreadyProcessed() {
        Recommendation rec = pendingRec(RecommendationSourceType.QUEUE_CARD, 99L, "과제", "14:00", "14:30");
        rec.accept(99L); // 이미 ACCEPTED
        when(recommendationRepository.findByRecommendIdAndMemberId(500L, MEMBER_ID)).thenReturn(Optional.of(rec));

        assertThatThrownBy(() -> service.accept(MEMBER_ID, 500L, false, null))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.RECOMMENDATION_ALREADY_PROCESSED);
        verify(scheduleRepository, never()).findByScheduleIdAndMemberId(anyLong(), anyLong());
        verify(scheduleRepository, never()).save(any());
    }
}
