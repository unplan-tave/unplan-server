package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.dto.request.ConditionRequest;
import com.unplan.unplanserver.domain.measurement.dto.response.ConditionResponse;
import com.unplan.unplanserver.domain.measurement.entity.Condition;
import com.unplan.unplanserver.domain.measurement.repository.ConditionRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConditionService {

    private final ConditionRepository conditionRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ConditionResponse createCondition(Long memberId, ConditionRequest.ConditionCreate request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Condition condition = new Condition(
                member,
                request.getConditionType(),
                request.getScore()
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
        Condition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 컨디션 기록입니다."));

        if (!condition.getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("해당 기록에 대한 권한이 없습니다.");
        }

        condition.update(
                condition.getConditionType(),
                request.getScore()
        );

        return ConditionResponse.from(condition);
    }

    @Transactional
    public void deleteCondition(Long memberId, Long conditionId) {
        Condition condition = conditionRepository.findById(conditionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 컨디션 기록입니다."));

        if (!condition.getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("해당 기록에 대한 권한이 없습니다.");
        }

        conditionRepository.delete(condition);
    }
}