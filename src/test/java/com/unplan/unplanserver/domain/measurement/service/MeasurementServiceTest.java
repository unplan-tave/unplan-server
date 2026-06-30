package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.AverageItem;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.MeasurementAverageResponse;
import com.unplan.unplanserver.domain.measurement.entity.Condition;
import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import com.unplan.unplanserver.domain.measurement.repository.ConditionRepository;
import com.unplan.unplanserver.domain.measurement.repository.SleepRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.domain.onboarding.dto.response.BiorhythmResponse;
import com.unplan.unplanserver.domain.onboarding.dto.response.SleepConditionResponse;
import com.unplan.unplanserver.domain.onboarding.service.BiorhythmService;
import com.unplan.unplanserver.domain.onboarding.service.SleepConditionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private SleepRepository sleepRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SleepConditionService sleepConditionService;

    @Mock
    private BiorhythmService biorhythmService;

    @InjectMocks
    private MeasurementService measurementService;

    @Test
    void getDailyRecordReturnsConditionsAndSleepsForDate() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Condition condition = new Condition(
                member,
                3,
                2,
                LocalDateTime.of(2026, 6, 24, 10, 0)
        );
        Sleep sleep = new Sleep(
                member,
                420,
                LocalDateTime.of(2026, 6, 23, 23, 30),
                LocalDateTime.of(2026, 6, 24, 6, 30),
                false
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of(condition));
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of(sleep));
        when(sleepRepository.findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
                memberId,
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(sleep));
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.date()).isEqualTo(date);
        assertThat(response.bodyScorePercent()).isEqualTo(50);
        assertThat(response.mindScorePercent()).isEqualTo(33);
        assertThat(response.sleepScore()).isEqualTo(84);
        assertThat(response.finalConditionScore()).isEqualTo(50);
        assertThat(response.sleepDurationMinutes()).isEqualTo(420);
        assertThat(response.conditions()).hasSize(1);
        assertThat(response.conditions().get(0).bodyScorePercent()).isEqualTo(50);
        assertThat(response.sleeps()).hasSize(1);
        assertThat(response.sleeps().get(0).durationMinutes()).isEqualTo(420);
    }

    @Test
    void getDailyRecordUsesDefaultScoresWhenNoRecordsExist() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.minusDays(1).atStartOfDay(),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.bodyScorePercent()).isEqualTo(50);
        assertThat(response.mindScorePercent()).isEqualTo(50);
        assertThat(response.sleepScore()).isEqualTo(70);
        assertThat(response.finalConditionScore()).isEqualTo(54);
        assertThat(response.conditions()).isEmpty();
        assertThat(response.sleeps()).isEmpty();
    }

    @Test
    void getDailyRecordUsesRecentConditionWithinPrevious24Hours() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Condition recentCondition = new Condition(
                member,
                6,
                6,
                LocalDateTime.of(2026, 6, 23, 23, 0)
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(Optional.of(recentCondition));
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.minusDays(1).atStartOfDay(),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.bodyScorePercent()).isEqualTo(100);
        assertThat(response.mindScorePercent()).isEqualTo(100);
        assertThat(response.sleepScore()).isEqualTo(70);
        assertThat(response.finalConditionScore()).isEqualTo(94);
    }

    @Test
    void getDailyRecordSetsSleepScoreZeroWhenZeroDurationSleepExists() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Sleep zeroDurationSleep = new Sleep(
                member,
                0,
                LocalDateTime.of(2026, 6, 24, 7, 0),
                LocalDateTime.of(2026, 6, 24, 7, 0),
                false
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of(zeroDurationSleep));

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isZero();
        assertThat(response.sleepDurationMinutes()).isZero();
    }

    @Test
    void getDailyRecordUsesZeroPatternScoreWhenOnlyNapExists() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Sleep nap = new Sleep(
                member,
                60,
                LocalDateTime.of(2026, 6, 24, 13, 0),
                LocalDateTime.of(2026, 6, 24, 14, 0),
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of(nap));
        when(sleepRepository.findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
                memberId,
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(nap));
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isEqualTo(24);
        assertThat(response.sleepDurationMinutes()).isEqualTo(60);
    }

    @Test
    void getDailyRecordUsesOnboardingSleepTargetWhenCalculatingSleepScore() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Sleep sleep = new Sleep(
                member,
                360,
                LocalDateTime.of(2026, 6, 23, 22, 0),
                LocalDateTime.of(2026, 6, 24, 4, 0),
                false
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of(sleep));
        when(sleepRepository.findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
                memberId,
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(sleep));
        stubSleepTarget(memberId, 360, "111100000000000000000011");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isEqualTo(94);
    }

    @Test
    void getDailyRecordUsesSleepTimelineAcrossMidnightTargetTimes() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Sleep sleep = new Sleep(
                member,
                480,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 24, 7, 0),
                false
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of(sleep));
        when(sleepRepository.findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
                memberId,
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(sleep));
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isEqualTo(94);
    }

    @Test
    void getDailyRecordUsesFirstOneAsBedTimeAndNextZeroAsWakeUpTime() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Sleep sleep = new Sleep(
                member,
                360,
                LocalDateTime.of(2026, 6, 23, 22, 0),
                LocalDateTime.of(2026, 6, 24, 4, 0),
                false
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of(sleep));
        when(sleepRepository.findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
                memberId,
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(sleep));
        stubSleepTarget(memberId, 360, "111100000000000000000011");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isEqualTo(94);
    }

    @Test
    void getDailyRecordUsesTargetDurationAsMinutes() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Sleep sleep = new Sleep(
                member,
                360,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 24, 7, 0),
                false
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of(sleep));
        when(sleepRepository.findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
                memberId,
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(sleep));
        stubSleepTarget(memberId, 360, "111111100000000000000001");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isEqualTo(94);
    }

    @Test
    void getDailyRecordFallsBackWhenSleepTimelineIsEmpty() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Sleep sleep = new Sleep(
                member,
                480,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 24, 7, 0),
                false
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtBetween(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay().minusNanos(1)
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeBetween(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay().minusNanos(1)
        )).thenReturn(List.of(sleep));
        when(sleepRepository.findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
                memberId,
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(sleep));
        stubSleepTarget(memberId, 480, "000000000000000000000000");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isEqualTo(94);
    }

    @Test
    void getAverageRecordsGroupsByDay() {
        Long memberId = 1L;
        MeasurementService service = spy(measurementService);
        LocalDate first = LocalDate.of(2026, 5, 1);
        LocalDate second = LocalDate.of(2026, 5, 2);

        doReturn(dailyRecord(first, 80, 70, 60, 90, 400))
                .when(service).getDailyRecord(memberId, first);
        doReturn(dailyRecord(second, 60, 50, 40, 70, 420))
                .when(service).getDailyRecord(memberId, second);

        MeasurementAverageResponse response = service.getAverageRecords(
                memberId,
                "2026-05-01",
                "2026-05-02",
                "ALL",
                "DAY"
        );

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).periodStart()).isEqualTo(first);
        assertThat(response.items().get(0).periodEnd()).isEqualTo(first);
        assertThat(response.items().get(0).label()).isEqualTo("5/1");
        assertThat(response.items().get(0).finalConditionScoreAverage()).isEqualTo(80);
        assertThat(response.items().get(1).label()).isEqualTo("5/2");
    }

    @Test
    void getAverageRecordsGroupsByWeekFromSundayToSaturday() {
        Long memberId = 1L;
        MeasurementService service = spy(measurementService);
        stubDailyRecords(
                service,
                memberId,
                LocalDate.of(2026, 4, 26),
                LocalDate.of(2026, 6, 6),
                70,
                60,
                50,
                80,
                410
        );

        MeasurementAverageResponse response = service.getAverageRecords(
                memberId,
                "2026-05-01",
                "2026-05-31",
                "ALL",
                "WEEK"
        );

        assertThat(response.items()).hasSize(6);
        AverageItem firstWeek = response.items().get(0);
        assertThat(firstWeek.periodStart()).isEqualTo(LocalDate.of(2026, 4, 26));
        assertThat(firstWeek.periodEnd()).isEqualTo(LocalDate.of(2026, 5, 2));
        assertThat(firstWeek.label()).isEqualTo("5월 1주");
    }

    @Test
    void getAverageRecordsGroupsByMonth() {
        Long memberId = 1L;
        MeasurementService service = spy(measurementService);
        stubDailyRecords(
                service,
                memberId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 30),
                70,
                60,
                50,
                80,
                410
        );

        MeasurementAverageResponse response = service.getAverageRecords(
                memberId,
                "2026-05-10",
                "2026-06-20",
                "ALL",
                "MONTH"
        );

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).periodStart()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(response.items().get(0).periodEnd()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(response.items().get(0).label()).isEqualTo("2026.05");
        assertThat(response.items().get(1).label()).isEqualTo("2026.06");
    }

    @Test
    void getAverageRecordsExcludesFutureDates() {
        Long memberId = 1L;
        MeasurementService service = spy(measurementService);
        LocalDate today = LocalDate.now();

        doReturn(dailyRecord(today, 80, 70, 60, 90, 400))
                .when(service).getDailyRecord(memberId, today);

        MeasurementAverageResponse response = service.getAverageRecords(
                memberId,
                today.toString(),
                today.plusDays(2).toString(),
                "ALL",
                "DAY"
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).periodStart()).isEqualTo(today);
    }

    @Test
    void getAverageRecordsReturnsAllTypeFields() {
        Long memberId = 1L;
        MeasurementService service = spy(measurementService);
        LocalDate date = LocalDate.of(2026, 5, 1);

        doReturn(dailyRecord(date, 80, 70, 60, 90, 400))
                .when(service).getDailyRecord(memberId, date);

        AverageItem item = service.getAverageRecords(
                memberId,
                "2026-05-01",
                "2026-05-01",
                "ALL",
                "DAY"
        ).items().get(0);

        assertThat(item.finalConditionScoreAverage()).isEqualTo(80);
        assertThat(item.bodyScorePercentAverage()).isEqualTo(70);
        assertThat(item.mindScorePercentAverage()).isEqualTo(60);
        assertThat(item.sleepScoreAverage()).isEqualTo(90);
        assertThat(item.sleepDurationMinutesAverage()).isEqualTo(400);
    }

    @Test
    void getAverageRecordsReturnsConditionTypeFieldsOnly() {
        Long memberId = 1L;
        MeasurementService service = spy(measurementService);
        LocalDate date = LocalDate.of(2026, 5, 1);

        doReturn(dailyRecord(date, 80, 70, 60, 90, 400))
                .when(service).getDailyRecord(memberId, date);

        AverageItem item = service.getAverageRecords(
                memberId,
                "2026-05-01",
                "2026-05-01",
                "CONDITION",
                "DAY"
        ).items().get(0);

        assertThat(item.finalConditionScoreAverage()).isEqualTo(80);
        assertThat(item.bodyScorePercentAverage()).isEqualTo(70);
        assertThat(item.mindScorePercentAverage()).isEqualTo(60);
        assertThat(item.sleepScoreAverage()).isNull();
        assertThat(item.sleepDurationMinutesAverage()).isNull();
    }

    @Test
    void getAverageRecordsReturnsSleepTypeFieldsOnly() {
        Long memberId = 1L;
        MeasurementService service = spy(measurementService);
        LocalDate date = LocalDate.of(2026, 5, 1);

        doReturn(dailyRecord(date, 80, 70, 60, 90, 400))
                .when(service).getDailyRecord(memberId, date);

        AverageItem item = service.getAverageRecords(
                memberId,
                "2026-05-01",
                "2026-05-01",
                "SLEEP",
                "DAY"
        ).items().get(0);

        assertThat(item.finalConditionScoreAverage()).isNull();
        assertThat(item.bodyScorePercentAverage()).isNull();
        assertThat(item.mindScorePercentAverage()).isNull();
        assertThat(item.sleepScoreAverage()).isEqualTo(90);
        assertThat(item.sleepDurationMinutesAverage()).isEqualTo(400);
    }

    @Test
    void getAverageRecordsThrowsWhenFromIsAfterTo() {
        assertThatThrownBy(() -> measurementService.getAverageRecords(
                1L,
                "2026-05-02",
                "2026-05-01",
                "ALL",
                "DAY"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAverageRecordsThrowsWhenTypeOrGroupByIsInvalid() {
        assertThatThrownBy(() -> measurementService.getAverageRecords(
                1L,
                "2026-05-01",
                "2026-05-02",
                "BAD",
                "DAY"
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> measurementService.getAverageRecords(
                1L,
                "2026-05-01",
                "2026-05-02",
                "ALL",
                "BAD"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private void stubDailyRecords(
            MeasurementService service,
            Long memberId,
            LocalDate start,
            LocalDate end,
            int finalConditionScore,
            int bodyScorePercent,
            int mindScorePercent,
            int sleepScore,
            int sleepDurationMinutes
    ) {
        LocalDate current = start;
        while (!current.isAfter(end)) {
            doReturn(dailyRecord(
                    current,
                    finalConditionScore,
                    bodyScorePercent,
                    mindScorePercent,
                    sleepScore,
                    sleepDurationMinutes
            )).when(service).getDailyRecord(memberId, current);
            current = current.plusDays(1);
        }
    }

    private MeasurementRecordResponse dailyRecord(
            LocalDate date,
            int finalConditionScore,
            int bodyScorePercent,
            int mindScorePercent,
            int sleepScore,
            int sleepDurationMinutes
    ) {
        return new MeasurementRecordResponse(
                date,
                finalConditionScore,
                "집중 가능",
                "일상 작업",
                bodyScorePercent,
                mindScorePercent,
                sleepScore,
                sleepDurationMinutes,
                List.of(),
                List.of()
        );
    }

    private void stubSleepTarget(Long memberId, int targetDuration, String sleepTimeline) {
        when(sleepConditionService.getSleepCondition(memberId))
                .thenReturn(new SleepConditionResponse(memberId, targetDuration, List.of()));
        when(biorhythmService.getBiorhythm(memberId))
                .thenReturn(new BiorhythmResponse.GetBiorhythm(
                        memberId,
                        "000000000000000000000000",
                        "000000000000000000000000",
                        sleepTimeline
                ));
    }
}
