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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SleepService {

    private static final int MAX_NAP_DURATION_MINUTES = 180;

    private final SleepRepository sleepRepository;
    private final MemberRepository memberRepository;
    private final ConditionRepository conditionRepository;

    @Transactional
    public SleepResponse createSleep(Long memberId, SleepRequest.SleepCreate request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        LocalDateTime bedTime = request.wakeUpTime()
                .minusMinutes(request.durationMinutes());

        boolean hasExistingCondition = conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(member, bedTime, request.wakeUpTime());

        if (hasExistingCondition) {
            throw new CustomException(ErrorCode.SLEEP_TIME_OVERLAP);
        }

        Boolean isNap = request.isNap();
        if (isNap == null) {
            isNap = request.durationMinutes() <= MAX_NAP_DURATION_MINUTES;
        }

        Sleep sleep = new Sleep(
                member,
                request.durationMinutes(),
                bedTime,
                request.wakeUpTime(),
                isNap
        );

        Sleep savedSleep = sleepRepository.save(sleep);

        return SleepResponse.from(savedSleep);
    }

    @Transactional
    public SleepResponse updateSleep(
            Long memberId,
            Long sleepId,
            SleepRequest.SleepUpdate request
    ) {
        Sleep sleep = sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SLEEP_NOT_FOUND));

        LocalDateTime bedTime = request.wakeUpTime()
                .minusMinutes(request.durationMinutes());

        boolean hasExistingCondition = conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(sleep.getMember(), bedTime, request.wakeUpTime());

        if (hasExistingCondition) {
            throw new CustomException(ErrorCode.SLEEP_TIME_OVERLAP);
        }

        Boolean isNap = request.isNap();
        if (isNap == null) {
            isNap = request.durationMinutes() <= MAX_NAP_DURATION_MINUTES;
        }

        sleep.updateSleep(
                request.durationMinutes(),
                bedTime,
                request.wakeUpTime(),
                isNap
        );

        return SleepResponse.from(sleep);
    }

    @Transactional
    public void deleteSleep(Long memberId, Long sleepId) {

        Sleep sleep = sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SLEEP_NOT_FOUND));

        sleepRepository.delete(sleep);

        // TODO: 기록 조회/흐름 조회 API 구현 시 수면 패널티 및 종합 컨디션 점수 재계산 로직 연결
    }
}