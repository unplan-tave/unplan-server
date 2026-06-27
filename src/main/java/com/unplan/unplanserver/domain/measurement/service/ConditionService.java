package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.dto.request.ConditionRequest;
import com.unplan.unplanserver.domain.measurement.dto.response.ConditionResponse;
import com.unplan.unplanserver.domain.measurement.entity.Condition;
import com.unplan.unplanserver.domain.measurement.repository.ConditionRepository;
import com.unplan.unplanserver.domain.measurement.repository.SleepRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConditionService {

    private final ConditionRepository conditionRepository;
    private final MemberRepository memberRepository;
    private final SleepRepository sleepRepository;

    @Transactional
    public ConditionResponse createCondition(Long memberId, ConditionRequest.ConditionCreate request) {

        if (sleepRepository.existsByMemberIdAndSleepTimeOverlap(memberId, request.getDateTime())) {
            throw new CustomException(ErrorCode.SLEEP_TIME_OVERLAP);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Condition condition = new Condition(
                member,
                request.getBodyScore(),
                request.getMindScore(),
                request.getDateTime()
        );

        Condition savedCondition = conditionRepository.save(condition);

        return ConditionResponse.from(savedCondition);
    }

    @Transactional
    public ConditionResponse updateCondition(
            Long memberId,
            Long conditionId,
            ConditionRequest.ConditionUpdate request
    ) {
        Condition condition = conditionRepository.findByConditionIdAndMemberMemberId(conditionId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONDITION_NOT_FOUND));

        if (sleepRepository.existsByMemberIdAndSleepTimeOverlap(memberId, request.getDateTime())) {
            throw new CustomException(ErrorCode.SLEEP_TIME_OVERLAP);
        }

        condition.updateScoresAndDateTime(
                request.getBodyScore(),
                request.getMindScore(),
                request.getDateTime()
        );

        return ConditionResponse.from(condition);
    }

    @Transactional
    public void deleteCondition(Long memberId, Long conditionId) {
        Condition condition = conditionRepository.findByConditionIdAndMemberMemberId(conditionId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.CONDITION_NOT_FOUND));

        conditionRepository.delete(condition);
    }
}