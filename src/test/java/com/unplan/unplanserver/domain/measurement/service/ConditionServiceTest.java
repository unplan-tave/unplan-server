package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.dto.response.ConditionResponse;
import com.unplan.unplanserver.domain.measurement.entity.Condition;
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
class ConditionServiceTest {

    @Mock
    private ConditionRepository conditionRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SleepRepository sleepRepository;

    @InjectMocks
    private ConditionService conditionService;

    @Test
    void getConditionReturnsConditionWhenItBelongsToMember() {
        Long memberId = 1L;
        Long conditionId = 12L;
        LocalDateTime measuredAt = LocalDateTime.of(2026, 6, 24, 22, 2);
        Condition condition = new Condition(new Member(), 4, 3, measuredAt);

        when(conditionRepository.findByConditionIdAndMemberMemberId(conditionId, memberId))
                .thenReturn(Optional.of(condition));

        ConditionResponse response = conditionService.getCondition(memberId, conditionId);

        assertThat(response.getBodyScore()).isEqualTo(4);
        assertThat(response.getMindScore()).isEqualTo(3);
        assertThat(response.getDateTime()).isEqualTo(measuredAt);
        verify(conditionRepository).findByConditionIdAndMemberMemberId(conditionId, memberId);
    }

    @Test
    void getConditionThrowsNotFoundWhenRecordDoesNotExistOrBelongToMember() {
        Long memberId = 1L;
        Long conditionId = 12L;

        when(conditionRepository.findByConditionIdAndMemberMemberId(conditionId, memberId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conditionService.getCondition(memberId, conditionId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CONDITION_NOT_FOUND);
    }
}
