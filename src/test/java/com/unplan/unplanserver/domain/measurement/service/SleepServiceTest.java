package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.dto.request.SleepRequest;
import com.unplan.unplanserver.domain.measurement.dto.response.SleepResponse;
import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import com.unplan.unplanserver.domain.measurement.repository.ConditionRepository;
import com.unplan.unplanserver.domain.measurement.repository.SleepRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SleepServiceTest {

    @Mock
    private SleepRepository sleepRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ConditionRepository conditionRepository;

    @InjectMocks
    private SleepService sleepService;

    @Test
    void getSleepReturnsSleepWhenItBelongsToMember() {
        Long memberId = 1L;
        Long sleepId = 45L;
        LocalDateTime bedTime = LocalDateTime.of(2026, 6, 23, 23, 0);
        LocalDateTime wakeUpTime = LocalDateTime.of(2026, 6, 24, 7, 30);
        Sleep sleep = new Sleep(new Member(), 450, bedTime, wakeUpTime, false);

        when(sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId))
                .thenReturn(Optional.of(sleep));

        SleepResponse response = sleepService.getSleep(memberId, sleepId);

        assertThat(response.getDurationMinutes()).isEqualTo(450);
        assertThat(response.getBedTime()).isEqualTo(bedTime);
        assertThat(response.getWakeUpTime()).isEqualTo(wakeUpTime);
        assertThat(response.getIsNap()).isFalse();
        assertThat(response.getIsAllNight()).isFalse();
        verify(sleepRepository).findBySleepIdAndMemberMemberId(sleepId, memberId);
    }

    @Test
    void createSleepCalculatesDurationMinutesFromBedTimeAndWakeUpTime() {
        Long memberId = 1L;
        Member member = new Member();
        LocalDateTime now = LocalDateTime.now();
        SleepRequest.SleepCreate request = new SleepRequest.SleepCreate(
                now.minusMinutes(610),
                now.minusMinutes(10),
                false,
                false
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(
                member,
                request.bedTime(),
                request.wakeUpTime()
        )).thenReturn(false);
        when(sleepRepository.save(any(Sleep.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SleepResponse response = sleepService.createSleep(memberId, request);

        assertThat(response.getDurationMinutes()).isEqualTo(600);
        assertThat(response.getBedTime()).isEqualTo(request.bedTime());
        assertThat(response.getWakeUpTime()).isEqualTo(request.wakeUpTime());
        assertThat(response.getIsNap()).isFalse();
        assertThat(response.getIsAllNight()).isFalse();
    }

    @Test
    void createSleepStoresZeroDurationWhenAllNightIsTrue() {
        Long memberId = 1L;
        Member member = new Member();
        SleepRequest.SleepCreate request = new SleepRequest.SleepCreate(
                LocalDateTime.of(2026, 6, 18, 0, 0),
                LocalDateTime.of(2026, 6, 19, 0, 0),
                false,
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(
                member,
                request.bedTime(),
                request.wakeUpTime()
        )).thenReturn(false);
        when(sleepRepository.save(any(Sleep.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SleepResponse response = sleepService.createSleep(memberId, request);

        assertThat(response.getDurationMinutes()).isZero();
        assertThat(response.getIsNap()).isFalse();
        assertThat(response.getIsAllNight()).isTrue();
    }

    @Test
    void createSleepThrowsWhenNapAndAllNightAreBothTrue() {
        Long memberId = 1L;
        Member member = new Member();
        SleepRequest.SleepCreate request = new SleepRequest.SleepCreate(
                LocalDateTime.of(2026, 6, 18, 0, 0),
                LocalDateTime.of(2026, 6, 19, 0, 0),
                true,
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> sleepService.createSleep(memberId, request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void updateSleepRecalculatesDurationMinutes() {
        Long memberId = 1L;
        Long sleepId = 45L;
        Member member = new Member();
        Sleep sleep = new Sleep(
                member,
                450,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 24, 6, 30),
                false
        );
        SleepRequest.SleepUpdate request = new SleepRequest.SleepUpdate(
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 24, 7, 0),
                false,
                false
        );

        when(sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId))
                .thenReturn(Optional.of(sleep));
        when(conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(
                member,
                request.bedTime(),
                request.wakeUpTime()
        )).thenReturn(false);

        SleepResponse response = sleepService.updateSleep(memberId, sleepId, request);

        assertThat(response.getDurationMinutes()).isEqualTo(480);
        assertThat(response.getBedTime()).isEqualTo(request.bedTime());
        assertThat(response.getWakeUpTime()).isEqualTo(request.wakeUpTime());
        assertThat(response.getIsAllNight()).isFalse();
    }

    @Test
    void getSleepThrowsNotFoundWhenRecordDoesNotExistOrBelongToMember() {
        Long memberId = 1L;
        Long sleepId = 45L;

        when(sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> sleepService.getSleep(memberId, sleepId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SLEEP_NOT_FOUND);
    }
}
