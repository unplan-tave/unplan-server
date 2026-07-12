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
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
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
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(condition));
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
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
        assertThat(response.conditions().data()).hasSize(1);
        assertThat(response.conditions().data().get(0).bodyScorePercent()).isEqualTo(50);
        assertThat(response.sleeps().data()).hasSize(1);
        assertThat(response.sleeps().data().get(0).durationMinutes()).isEqualTo(420);
        assertThat(response.sleeps().data().get(0).isAllNight()).isFalse();
    }

    @Test
    void getDailyRecordUsesDefaultScoresWhenNoRecordsExist() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.minusDays(1).atStartOfDay(),
                date.atStartOfDay()
        )).thenReturn(List.of());

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.bodyScorePercent()).isEqualTo(50);
        assertThat(response.mindScorePercent()).isEqualTo(50);
        assertThat(response.sleepScore()).isEqualTo(70);
        assertThat(response.finalConditionScore()).isEqualTo(54);
        assertThat(response.conditions().data()).isEmpty();
        assertThat(response.sleeps().data()).isEmpty();
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
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.of(recentCondition));
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.minusDays(1).atStartOfDay(),
                date.atStartOfDay()
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
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(zeroDurationSleep));

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isZero();
        assertThat(response.sleepDurationMinutes()).isZero();
    }

    @Test
    void getDailyRecordSetsSleepScoreZeroWhenAllNightSleepExists() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Sleep allNightSleep = new Sleep(
                member,
                0,
                LocalDateTime.of(2026, 6, 24, 0, 0),
                LocalDateTime.of(2026, 6, 24, 0, 1),
                false,
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(allNightSleep));

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isZero();
        assertThat(response.sleeps().data()).hasSize(1);
        assertThat(response.sleeps().data().get(0).isAllNight()).isTrue();
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
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
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
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
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
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
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
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
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
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
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
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
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
    void getDailyRecordFallsBackToDefaultSleepTimelineWhenBiorhythmDoesNotExist() {
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
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                member,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());
        when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                memberId,
                date.atStartOfDay().minusHours(24),
                date.atStartOfDay()
        )).thenReturn(Optional.empty());
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                memberId,
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(sleep));
        when(sleepRepository.findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
                memberId,
                date.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(sleep));
        when(sleepConditionService.getSleepCondition(memberId))
                .thenReturn(new SleepConditionResponse(memberId, 480, List.of()));
        when(biorhythmService.getBiorhythm(memberId))
                .thenThrow(new CustomException(ErrorCode.INVALID_REQUEST));

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isEqualTo(94);
    }

    @Test
    void getDailyRecordExcludesConditionAndSleepAtNextDayMidnight() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Condition conditionAtDate = new Condition(
                member,
                3,
                3,
                LocalDateTime.of(2026, 6, 24, 23, 59)
        );
        Condition conditionAtNextDayMidnight = new Condition(
                member,
                6,
                6,
                LocalDateTime.of(2026, 6, 25, 0, 0)
        );
        Sleep sleepAtDate = new Sleep(
                member,
                60,
                LocalDateTime.of(2026, 6, 24, 12, 0),
                LocalDateTime.of(2026, 6, 24, 13, 0),
                true
        );
        Sleep sleepAtNextDayMidnight = new Sleep(
                member,
                480,
                LocalDateTime.of(2026, 6, 24, 16, 0),
                LocalDateTime.of(2026, 6, 25, 0, 0),
                false
        );

        stubRangeBasedMeasurementData(
                memberId,
                member,
                List.of(conditionAtDate, conditionAtNextDayMidnight),
                List.of(sleepAtDate, sleepAtNextDayMidnight)
        );
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.conditions().data()).hasSize(1);
        assertThat(response.conditions().data().get(0).dateTime()).isEqualTo(conditionAtDate.getMeasuredAt());
        assertThat(response.sleeps().data()).hasSize(1);
        assertThat(response.sleeps().data().get(0).wakeUpTime()).isEqualTo(sleepAtDate.getWakeUpTime());
        assertThat(response.sleepDurationMinutes()).isEqualTo(60);
    }

    @Test
    void getDailyRecordUsesFirstPageWhenPagesAreNull() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        List<Condition> conditions = createConditions(member, date, 35);
        List<Sleep> sleeps = createSleeps(member, date, 35);

        stubRangeBasedMeasurementData(memberId, member, conditions, sleeps);
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date, null, null);

        assertThat(response.conditions().pagination().page()).isZero();
        assertThat(response.conditions().pagination().size()).isEqualTo(30);
        assertThat(response.conditions().data()).hasSize(30);
        assertThat(response.sleeps().pagination().page()).isZero();
        assertThat(response.sleeps().pagination().size()).isEqualTo(30);
        assertThat(response.sleeps().data()).hasSize(30);
    }

    @Test
    void getDailyRecordCorrectsNegativePagesToFirstPage() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        List<Condition> conditions = createConditions(member, date, 35);
        List<Sleep> sleeps = createSleeps(member, date, 35);

        stubRangeBasedMeasurementData(memberId, member, conditions, sleeps);
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date, -1, -5);

        assertThat(response.conditions().pagination().page()).isZero();
        assertThat(response.conditions().pagination().size()).isEqualTo(30);
        assertThat(response.conditions().data()).hasSize(30);
        assertThat(response.sleeps().pagination().page()).isZero();
        assertThat(response.sleeps().pagination().size()).isEqualTo(30);
        assertThat(response.sleeps().data()).hasSize(30);
    }

    @Test
    void getDailyRecordPaginatesConditionsByThirtyItemsAndKeepsScoreBasedOnAllRecords() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        List<Condition> conditions = createConditions(member, date, 35);
        List<Sleep> sleeps = createSleeps(member, date, 3);

        stubRangeBasedMeasurementData(memberId, member, conditions, sleeps);
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        MeasurementRecordResponse firstPage = measurementService.getDailyRecord(memberId, date, 0, 0);
        MeasurementRecordResponse secondPage = measurementService.getDailyRecord(memberId, date, 1, 0);

        assertThat(firstPage.conditions().data()).hasSize(30);
        assertThat(firstPage.conditions().pagination().page()).isZero();
        assertThat(firstPage.conditions().pagination().size()).isEqualTo(30);
        assertThat(firstPage.conditions().pagination().totalElements()).isEqualTo(35);
        assertThat(firstPage.conditions().pagination().totalPages()).isEqualTo(2);
        assertThat(firstPage.conditions().pagination().hasNext()).isTrue();
        assertThat(firstPage.conditions().pagination().hasPrevious()).isFalse();
        assertThat(secondPage.conditions().data()).hasSize(5);
        assertThat(secondPage.conditions().pagination().page()).isEqualTo(1);
        assertThat(secondPage.conditions().pagination().hasNext()).isFalse();
        assertThat(secondPage.conditions().pagination().hasPrevious()).isTrue();
        assertDailyScoreUnchanged(firstPage, secondPage);
    }

    @Test
    void getDailyRecordPaginatesSleepsByThirtyItemsAndKeepsScoreBasedOnAllRecords() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        List<Condition> conditions = createConditions(member, date, 3);
        List<Sleep> sleeps = createSleeps(member, date, 35);

        stubRangeBasedMeasurementData(memberId, member, conditions, sleeps);
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        MeasurementRecordResponse firstPage = measurementService.getDailyRecord(memberId, date, 0, 0);
        MeasurementRecordResponse secondPage = measurementService.getDailyRecord(memberId, date, 0, 1);

        assertThat(firstPage.sleeps().data()).hasSize(30);
        assertThat(firstPage.sleeps().pagination().page()).isZero();
        assertThat(firstPage.sleeps().pagination().size()).isEqualTo(30);
        assertThat(firstPage.sleeps().pagination().totalElements()).isEqualTo(35);
        assertThat(firstPage.sleeps().pagination().totalPages()).isEqualTo(2);
        assertThat(firstPage.sleeps().pagination().hasNext()).isTrue();
        assertThat(firstPage.sleeps().pagination().hasPrevious()).isFalse();
        assertThat(secondPage.sleeps().data()).hasSize(5);
        assertThat(secondPage.sleeps().pagination().page()).isEqualTo(1);
        assertThat(secondPage.sleeps().pagination().hasNext()).isFalse();
        assertThat(secondPage.sleeps().pagination().hasPrevious()).isTrue();
        assertDailyScoreUnchanged(firstPage, secondPage);
    }

    @Test
    void getDailyRecordHandlesConditionPageAndSleepPageIndependently() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        List<Condition> conditions = createConditions(member, date, 35);
        List<Sleep> sleeps = createSleeps(member, date, 62);

        stubRangeBasedMeasurementData(memberId, member, conditions, sleeps);
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date, 1, 2);

        assertThat(response.conditions().data()).hasSize(5);
        assertThat(response.conditions().pagination().page()).isEqualTo(1);
        assertThat(response.conditions().pagination().totalElements()).isEqualTo(35);
        assertThat(response.conditions().pagination().totalPages()).isEqualTo(2);
        assertThat(response.sleeps().data()).hasSize(2);
        assertThat(response.sleeps().pagination().page()).isEqualTo(2);
        assertThat(response.sleeps().pagination().totalElements()).isEqualTo(62);
        assertThat(response.sleeps().pagination().totalPages()).isEqualTo(3);
    }

    @Test
    void getAverageRecordsGroupsByDay() {
        Long memberId = 1L;
        Member member = new Member();
        LocalDate first = LocalDate.of(2026, 5, 1);
        stubAveragePreloadedData(memberId, member, List.of(), List.of());

        MeasurementAverageResponse response = measurementService.getAverageRecords(
                memberId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                "ALL",
                "DAY"
        );

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).periodStart()).isEqualTo(first);
        assertThat(response.items().get(0).periodEnd()).isEqualTo(first);
        assertThat(response.items().get(0).label()).isEqualTo("5/1");
        assertThat(response.items().get(0).finalConditionScoreAverage()).isEqualTo(54);
        assertThat(response.items().get(1).label()).isEqualTo("5/2");
    }

    @Test
    void getAverageRecordsGroupsByWeekFromSundayToSaturday() {
        Long memberId = 1L;
        Member member = new Member();
        stubAveragePreloadedData(memberId, member, List.of(), List.of());

        MeasurementAverageResponse response = measurementService.getAverageRecords(
                memberId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
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
        Member member = new Member();
        stubAveragePreloadedData(memberId, member, List.of(), List.of());

        MeasurementAverageResponse response = measurementService.getAverageRecords(
                memberId,
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 6, 20),
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
        Member member = new Member();
        LocalDate today = LocalDate.now();
        stubAveragePreloadedData(memberId, member, List.of(), List.of());

        MeasurementAverageResponse response = measurementService.getAverageRecords(
                memberId,
                today,
                today.plusDays(2),
                "ALL",
                "DAY"
        );

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).periodStart()).isEqualTo(today);
    }

    @Test
    void getAverageRecordsReturnsAllTypeFields() {
        Long memberId = 1L;
        Member member = new Member();
        stubAveragePreloadedData(memberId, member, List.of(), List.of());

        AverageItem item = measurementService.getAverageRecords(
                memberId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                "ALL",
                "DAY"
        ).items().get(0);

        assertThat(item.finalConditionScoreAverage()).isEqualTo(54);
        assertThat(item.bodyScorePercentAverage()).isEqualTo(50);
        assertThat(item.mindScorePercentAverage()).isEqualTo(50);
        assertThat(item.sleepScoreAverage()).isEqualTo(70);
        assertThat(item.sleepDurationMinutesAverage()).isZero();
    }

    @Test
    void getAverageRecordsReturnsConditionTypeFieldsOnly() {
        Long memberId = 1L;
        Member member = new Member();
        stubAveragePreloadedData(memberId, member, List.of(), List.of());

        AverageItem item = measurementService.getAverageRecords(
                memberId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                "CONDITION",
                "DAY"
        ).items().get(0);

        assertThat(item.finalConditionScoreAverage()).isEqualTo(54);
        assertThat(item.bodyScorePercentAverage()).isEqualTo(50);
        assertThat(item.mindScorePercentAverage()).isEqualTo(50);
        assertThat(item.sleepScoreAverage()).isNull();
        assertThat(item.sleepDurationMinutesAverage()).isNull();
    }

    @Test
    void getAverageRecordsReturnsSleepTypeFieldsOnly() {
        Long memberId = 1L;
        Member member = new Member();
        stubAveragePreloadedData(memberId, member, List.of(), List.of());

        AverageItem item = measurementService.getAverageRecords(
                memberId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 1),
                "SLEEP",
                "DAY"
        ).items().get(0);

        assertThat(item.finalConditionScoreAverage()).isNull();
        assertThat(item.bodyScorePercentAverage()).isNull();
        assertThat(item.mindScorePercentAverage()).isNull();
        assertThat(item.sleepScoreAverage()).isEqualTo(70);
        assertThat(item.sleepDurationMinutesAverage()).isZero();
    }

    @Test
    void getAverageRecordsDoesNotCallGetDailyRecordForEachDate() {
        Long memberId = 1L;
        Member member = new Member();
        MeasurementService service = spy(measurementService);
        stubAveragePreloadedData(memberId, member, List.of(), List.of());

        service.getAverageRecords(
                memberId,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 3),
                "ALL",
                "DAY"
        );

        verify(service, never()).getDailyRecord(anyLong(), any(LocalDate.class));
    }

    @Test
    void getAverageRecordsForSingleDayMatchesDailyRecordWithFallbackPolicies() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Condition recentCondition = new Condition(
                member,
                6,
                2,
                LocalDateTime.of(2026, 6, 23, 23, 0)
        );
        Sleep previousDaySleep = new Sleep(
                member,
                450,
                LocalDateTime.of(2026, 6, 22, 23, 30),
                LocalDateTime.of(2026, 6, 23, 7, 0),
                false
        );
        Sleep recentStabilitySleep = new Sleep(
                member,
                480,
                LocalDateTime.of(2026, 6, 21, 23, 0),
                LocalDateTime.of(2026, 6, 22, 7, 0),
                false
        );
        Sleep oldSleepOutsideStabilityWindow = new Sleep(
                member,
                120,
                LocalDateTime.of(2026, 6, 1, 23, 0),
                LocalDateTime.of(2026, 6, 2, 7, 0),
                false
        );
        List<Condition> conditions = List.of(recentCondition);
        List<Sleep> sleeps = List.of(oldSleepOutsideStabilityWindow, recentStabilitySleep, previousDaySleep);

        stubRangeBasedMeasurementData(memberId, member, conditions, sleeps);
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        MeasurementRecordResponse dailyRecord = measurementService.getDailyRecord(memberId, date);
        AverageItem averageItem = measurementService.getAverageRecords(
                memberId,
                date,
                date,
                "ALL",
                "DAY"
        ).items().get(0);

        assertThat(dailyRecord.conditions().data()).isEmpty();
        assertThat(dailyRecord.sleeps().data()).isEmpty();
        assertThat(dailyRecord.bodyScorePercent()).isEqualTo(100);
        assertThat(dailyRecord.mindScorePercent()).isEqualTo(33);
        assertThat(dailyRecord.sleepDurationMinutes()).isZero();
        assertThat(averageItem.finalConditionScoreAverage()).isEqualTo(dailyRecord.finalConditionScore());
        assertThat(averageItem.bodyScorePercentAverage()).isEqualTo(dailyRecord.bodyScorePercent());
        assertThat(averageItem.mindScorePercentAverage()).isEqualTo(dailyRecord.mindScorePercent());
        assertThat(averageItem.sleepScoreAverage()).isEqualTo(dailyRecord.sleepScore());
        assertThat(averageItem.sleepDurationMinutesAverage()).isEqualTo(dailyRecord.sleepDurationMinutes());
    }

    @Test
    void getAverageRecordsExcludesConditionAndSleepAtCalculationEndMidnight() {
        Long memberId = 1L;
        LocalDate date = LocalDate.of(2026, 6, 24);
        Member member = new Member();
        Condition conditionAtDate = new Condition(
                member,
                3,
                3,
                LocalDateTime.of(2026, 6, 24, 23, 59)
        );
        Condition conditionAtNextDayMidnight = new Condition(
                member,
                6,
                6,
                LocalDateTime.of(2026, 6, 25, 0, 0)
        );
        Sleep sleepAtDate = new Sleep(
                member,
                60,
                LocalDateTime.of(2026, 6, 24, 12, 0),
                LocalDateTime.of(2026, 6, 24, 13, 0),
                true
        );
        Sleep sleepAtNextDayMidnight = new Sleep(
                member,
                480,
                LocalDateTime.of(2026, 6, 24, 16, 0),
                LocalDateTime.of(2026, 6, 25, 0, 0),
                false
        );

        stubRangeBasedMeasurementData(
                memberId,
                member,
                List.of(conditionAtDate, conditionAtNextDayMidnight),
                List.of(sleepAtDate, sleepAtNextDayMidnight)
        );
        stubSleepTarget(memberId, 480, "111111100000000000000001");

        AverageItem item = measurementService.getAverageRecords(
                memberId,
                date,
                date,
                "ALL",
                "DAY"
        ).items().get(0);

        assertThat(item.bodyScorePercentAverage()).isEqualTo(50);
        assertThat(item.mindScorePercentAverage()).isEqualTo(50);
        assertThat(item.sleepDurationMinutesAverage()).isEqualTo(60);
    }

    @Test
    void getAverageRecordsThrowsWhenFromIsAfterTo() {
        assertThatThrownBy(() -> measurementService.getAverageRecords(
                1L,
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 1),
                "ALL",
                "DAY"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAverageRecordsThrowsWhenTypeOrGroupByIsInvalid() {
        assertThatThrownBy(() -> measurementService.getAverageRecords(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                "BAD",
                "DAY"
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> measurementService.getAverageRecords(
                1L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                "ALL",
                "BAD"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    private List<Condition> createConditions(Member member, LocalDate date, int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> new Condition(
                        member,
                        3,
                        2,
                        date.atStartOfDay().plusMinutes(index)
                ))
                .toList();
    }

    private List<Sleep> createSleeps(Member member, LocalDate date, int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> new Sleep(
                        member,
                        10,
                        date.atStartOfDay().plusMinutes(index),
                        date.atStartOfDay().plusHours(1).plusMinutes(index),
                        false
                ))
                .toList();
    }

    private void assertDailyScoreUnchanged(
            MeasurementRecordResponse expected,
            MeasurementRecordResponse actual
    ) {
        assertThat(actual.finalConditionScore()).isEqualTo(expected.finalConditionScore());
        assertThat(actual.conditionLevel()).isEqualTo(expected.conditionLevel());
        assertThat(actual.conditionTag()).isEqualTo(expected.conditionTag());
        assertThat(actual.bodyScorePercent()).isEqualTo(expected.bodyScorePercent());
        assertThat(actual.mindScorePercent()).isEqualTo(expected.mindScorePercent());
        assertThat(actual.sleepScore()).isEqualTo(expected.sleepScore());
        assertThat(actual.sleepDurationMinutes()).isEqualTo(expected.sleepDurationMinutes());
    }

    private void stubAveragePreloadedData(
            Long memberId,
            Member member,
            List<Condition> conditions,
            List<Sleep> sleeps
    ) {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                eq(member),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(conditions);
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                eq(memberId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(sleeps);
        stubSleepTarget(memberId, 480, "111111100000000000000001");
    }

    private void stubRangeBasedMeasurementData(
            Long memberId,
            Member member,
            List<Condition> conditions,
            List<Sleep> sleeps
    ) {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.findAllByMemberAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThan(
                eq(member),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenAnswer(invocation -> {
            LocalDateTime start = invocation.getArgument(1);
            LocalDateTime end = invocation.getArgument(2);
            return conditions.stream()
                    .filter(condition -> !condition.getMeasuredAt().isBefore(start))
                    .filter(condition -> condition.getMeasuredAt().isBefore(end))
                    .toList();
        });
        lenient().when(conditionRepository.findTopByMemberMemberIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDesc(
                eq(memberId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenAnswer(invocation -> {
            LocalDateTime start = invocation.getArgument(1);
            LocalDateTime end = invocation.getArgument(2);
            return conditions.stream()
                    .filter(condition -> !condition.getMeasuredAt().isBefore(start))
                    .filter(condition -> condition.getMeasuredAt().isBefore(end))
                    .max((left, right) -> left.getMeasuredAt().compareTo(right.getMeasuredAt()));
        });
        when(sleepRepository.findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
                eq(memberId),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenAnswer(invocation -> {
            LocalDateTime start = invocation.getArgument(1);
            LocalDateTime end = invocation.getArgument(2);
            return sleeps.stream()
                    .filter(sleep -> !sleep.getWakeUpTime().isBefore(start))
                    .filter(sleep -> sleep.getWakeUpTime().isBefore(end))
                    .toList();
        });
        lenient().when(sleepRepository.findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
                eq(memberId),
                any(LocalDateTime.class)
        )).thenAnswer(invocation -> {
            LocalDateTime before = invocation.getArgument(1);
            return sleeps.stream()
                    .filter(sleep -> sleep.getWakeUpTime().isBefore(before))
                    .sorted((left, right) -> right.getWakeUpTime().compareTo(left.getWakeUpTime()))
                    .limit(7)
                    .toList();
        });
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
