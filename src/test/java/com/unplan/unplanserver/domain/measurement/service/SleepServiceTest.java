package com.unplan.unplanserver.domain.measurement.service;

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
        verify(sleepRepository).findBySleepIdAndMemberMemberId(sleepId, memberId);
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
