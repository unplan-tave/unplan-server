package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.dto.request.ConditionCreateRequest;
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
    public ConditionResponse createCondition(Long memberId, ConditionCreateRequest request) {
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
}