package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse;
import com.unplan.unplanserver.domain.measurement.entity.Condition;
import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import com.unplan.unplanserver.domain.measurement.repository.ConditionRepository;
import com.unplan.unplanserver.domain.measurement.repository.SleepRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeasurementServiceTest {

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private SleepRepository sleepRepository;

    @Mock
    private MemberRepository memberRepository;

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

        MeasurementRecordResponse response = measurementService.getDailyRecord(memberId, date);

        assertThat(response.sleepScore()).isEqualTo(24);
        assertThat(response.sleepDurationMinutes()).isEqualTo(60);
    }
}
