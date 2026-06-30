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

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SleepService {

    private static final int MAX_SLEEP_DURATION_MINUTES = 1440;

    private final SleepRepository sleepRepository;
    private final MemberRepository memberRepository;
    private final ConditionRepository conditionRepository;

    public SleepResponse getSleep(Long memberId, Long sleepId) {
        Sleep sleep = sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SLEEP_NOT_FOUND));

        return SleepResponse.from(sleep);
    }

    @Transactional
    public SleepResponse createSleep(Long memberId, SleepRequest.SleepCreate request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        SleepInput sleepInput = validateSleepInput(
                request.bedTime(),
                request.wakeUpTime(),
                request.isNap(),
                request.isAllNight()
        );

        boolean hasExistingCondition = !Boolean.TRUE.equals(request.isAllNight()) && conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(
                member,
                request.bedTime(),
                request.wakeUpTime()
        );

        if (hasExistingCondition) {
            throw new CustomException(ErrorCode.SLEEP_TIME_OVERLAP);
        }

        Sleep sleep = new Sleep(
                member,
                sleepInput.durationMinutes(),
                request.bedTime(),
                request.wakeUpTime(),
                request.isNap(),
                request.isAllNight()
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

        SleepInput sleepInput = validateSleepInput(
                request.bedTime(),
                request.wakeUpTime(),
                request.isNap(),
                request.isAllNight()
        );

        boolean hasExistingCondition = !Boolean.TRUE.equals(request.isAllNight()) && conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(
                sleep.getMember(),
                request.bedTime(),
                request.wakeUpTime()
        );

        if (hasExistingCondition) {
            throw new CustomException(ErrorCode.SLEEP_TIME_OVERLAP);
        }

        sleep.updateSleep(
                sleepInput.durationMinutes(),
                request.bedTime(),
                request.wakeUpTime(),
                request.isNap(),
                request.isAllNight()
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

    private SleepInput validateSleepInput(
            LocalDateTime bedTime,
            LocalDateTime wakeUpTime,
            Boolean isNap,
            Boolean isAllNight
    ) {
        if (bedTime == null || wakeUpTime == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        LocalDateTime now = LocalDateTime.now();
        if (bedTime.isAfter(now) || wakeUpTime.isAfter(now)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (!bedTime.isBefore(wakeUpTime)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (Boolean.TRUE.equals(isNap) && Boolean.TRUE.equals(isAllNight)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        long durationMinutes = Duration.between(bedTime, wakeUpTime).toMinutes();
        if (durationMinutes > MAX_SLEEP_DURATION_MINUTES) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        if (Boolean.TRUE.equals(isAllNight)) {
            return new SleepInput(0);
        }
        if (durationMinutes < 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return new SleepInput((int) durationMinutes);
    }

    private record SleepInput(int durationMinutes) {
    }
}
