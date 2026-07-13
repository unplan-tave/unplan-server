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
import java.util.List;
import java.util.UUID;

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

        boolean hasExistingCondition = !request.isAllNight() && conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(
                member,
                request.bedTime(),
                request.wakeUpTime()
        );
        if (hasExistingCondition) {
            throw new CustomException(ErrorCode.SLEEP_TIME_OVERLAP);
        }

        List<Sleep> sleeps = createSleepSegments(
                member,
                sleepInput.durationMinutes(),
                request.bedTime(),
                request.wakeUpTime(),
                request.isNap(),
                request.isAllNight()
        );

        List<Sleep> savedSleeps = sleepRepository.saveAll(sleeps);

        return SleepResponse.from(savedSleeps.get(0));
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

        boolean hasExistingCondition = !request.isAllNight() && conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(
                sleep.getMember(),
                request.bedTime(),
                request.wakeUpTime()
        );

        if (hasExistingCondition) {
            throw new CustomException(ErrorCode.SLEEP_TIME_OVERLAP);
        }

        if (sleep.isContinuousSleep() || sleepInput.durationMinutes() > MAX_SLEEP_DURATION_MINUTES) {
            List<Sleep> targetSleeps = findTargetSleeps(memberId, sleep);
            sleepRepository.deleteAll(targetSleeps);

            List<Sleep> replacementSleeps = createSleepSegments(
                    sleep.getMember(),
                    sleepInput.durationMinutes(),
                    request.bedTime(),
                    request.wakeUpTime(),
                    request.isNap(),
                    request.isAllNight()
            );

            return SleepResponse.from(sleepRepository.saveAll(replacementSleeps).get(0));
        }

        sleep.updateSleep(
                sleepInput.durationMinutes(),
                request.bedTime(),
                request.wakeUpTime(),
                request.isNap(),
                request.isAllNight(),
                sleepInput.durationMinutes(),
                request.bedTime(),
                request.wakeUpTime(),
                null
        );

        return SleepResponse.from(sleep);
    }

    @Transactional
    public void deleteSleep(Long memberId, Long sleepId) {

        Sleep sleep = sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SLEEP_NOT_FOUND));

        sleepRepository.deleteAll(findTargetSleeps(memberId, sleep));
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
        if (Boolean.TRUE.equals(isAllNight)) {
            return new SleepInput(0);
        }
        if (durationMinutes < 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        return new SleepInput((int) durationMinutes);
    }

    private List<Sleep> createSleepSegments(
            Member member,
            int totalDurationMinutes,
            LocalDateTime originalBedTime,
            LocalDateTime originalWakeUpTime,
            Boolean isNap,
            Boolean isAllNight
    ) {
        if (totalDurationMinutes <= MAX_SLEEP_DURATION_MINUTES) {
            return List.of(new Sleep(
                    member,
                    totalDurationMinutes,
                    originalBedTime,
                    originalWakeUpTime,
                    isNap,
                    isAllNight,
                    totalDurationMinutes,
                    originalBedTime,
                    originalWakeUpTime,
                    null
            ));
        }

        String groupId = UUID.randomUUID().toString();
        LocalDateTime segmentStart = originalBedTime;
        int remainingMinutes = totalDurationMinutes;
        List<Sleep> segments = new java.util.ArrayList<>();

        while (remainingMinutes > 0) {
            int segmentDurationMinutes = Math.min(remainingMinutes, MAX_SLEEP_DURATION_MINUTES);
            LocalDateTime segmentEnd = segmentStart.plusMinutes(segmentDurationMinutes);
            segments.add(new Sleep(
                    member,
                    segmentDurationMinutes,
                    segmentStart,
                    segmentEnd,
                    isNap,
                    false,
                    totalDurationMinutes,
                    originalBedTime,
                    originalWakeUpTime,
                    groupId
            ));

            remainingMinutes -= segmentDurationMinutes;
            segmentStart = segmentEnd;
        }

        return segments;
    }

    private List<Sleep> findTargetSleeps(Long memberId, Sleep sleep) {
        if (!sleep.isContinuousSleep()) {
            return List.of(sleep);
        }

        List<Sleep> targetSleeps = sleepRepository.findAllByMemberMemberIdAndContinuousSleepGroupId(
                memberId,
                sleep.getContinuousSleepGroupId()
        );
        if (targetSleeps.isEmpty()) {
            return List.of(sleep);
        }
        return targetSleeps;
    }

    private record SleepInput(int durationMinutes) {
    }
}
