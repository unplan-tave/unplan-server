package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.dto.request.SleepRequest;
import com.unplan.unplanserver.domain.measurement.dto.response.SleepResponse;
import com.unplan.unplanserver.domain.measurement.calculator.MeasurementCommentCalculator;
import com.unplan.unplanserver.domain.measurement.calculator.SleepTargetMinutesResolver;
import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import com.unplan.unplanserver.domain.measurement.repository.ConditionRepository;
import com.unplan.unplanserver.domain.measurement.repository.SleepRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.domain.onboarding.service.SleepConditionService;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final SleepConditionService sleepConditionService;

    public SleepResponse getSleep(Long memberId, Long sleepId) {
        Sleep sleep = sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SLEEP_NOT_FOUND));

        return toSleepResponse(memberId, sleep);
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

        return toSleepResponse(memberId, savedSleeps.get(0));
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
            return updateSleepSegments(
                    memberId,
                    sleep,
                    sleepInput.durationMinutes(),
                    request.bedTime(),
                    request.wakeUpTime(),
                    request.isNap(),
                    request.isAllNight()
            );
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

        return toSleepResponse(memberId, sleep);
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
        String groupId = totalDurationMinutes > MAX_SLEEP_DURATION_MINUTES
                ? UUID.randomUUID().toString()
                : null;

        return createSleepSegmentSpecs(
                totalDurationMinutes,
                originalBedTime,
                originalWakeUpTime,
                isNap,
                isAllNight,
                groupId
        ).stream()
                .map(segment -> new Sleep(
                        member,
                        segment.durationMinutes(),
                        segment.bedTime(),
                        segment.wakeUpTime(),
                        segment.isNap(),
                        segment.isAllNight(),
                        segment.totalDurationMinutes(),
                        segment.originalBedTime(),
                        segment.originalWakeUpTime(),
                        segment.continuousSleepGroupId()
                ))
                .toList();
    }

    private SleepResponse updateSleepSegments(
            Long memberId,
            Sleep targetSleep,
            int totalDurationMinutes,
            LocalDateTime originalBedTime,
            LocalDateTime originalWakeUpTime,
            Boolean isNap,
            Boolean isAllNight
    ) {
        List<Sleep> existingSegments = orderedTargetSleeps(memberId, targetSleep);
        String groupId = resolveContinuousSleepGroupId(targetSleep, totalDurationMinutes);
        List<SleepSegment> newSegments = createSleepSegmentSpecs(
                totalDurationMinutes,
                originalBedTime,
                originalWakeUpTime,
                isNap,
                isAllNight,
                groupId
        );

        updateSleep(targetSleep, newSegments.get(0));

        List<Sleep> reusableSegments = existingSegments.stream()
                .filter(existingSegment -> !isSameSleep(existingSegment, targetSleep))
                .toList();
        List<Sleep> additionalSegments = new ArrayList<>();
        for (int index = 1; index < newSegments.size(); index++) {
            SleepSegment segment = newSegments.get(index);
            int reusableIndex = index - 1;
            if (reusableIndex < reusableSegments.size()) {
                updateSleep(reusableSegments.get(reusableIndex), segment);
                continue;
            }

            additionalSegments.add(new Sleep(
                    targetSleep.getMember(),
                    segment.durationMinutes(),
                    segment.bedTime(),
                    segment.wakeUpTime(),
                    segment.isNap(),
                    segment.isAllNight(),
                    segment.totalDurationMinutes(),
                    segment.originalBedTime(),
                    segment.originalWakeUpTime(),
                    segment.continuousSleepGroupId()
            ));
        }

        if (newSegments.size() - 1 < reusableSegments.size()) {
            sleepRepository.deleteAll(reusableSegments.subList(newSegments.size() - 1, reusableSegments.size()));
        }
        if (!additionalSegments.isEmpty()) {
            sleepRepository.saveAll(additionalSegments);
        }

        return toSleepResponse(memberId, targetSleep);
    }

    private SleepResponse toSleepResponse(Long memberId, Sleep sleep) {
        int targetSleepMinutes = SleepTargetMinutesResolver.resolve(sleepConditionService, memberId);
        String sleepRecordComment = MeasurementCommentCalculator.calculateSleepRecordComment(
                sleep.getAllNight(),
                sleep.getNap(),
                sleep.getDurationMinutes(),
                targetSleepMinutes
        );
        return SleepResponse.from(sleep, sleepRecordComment);
    }

    private void updateSleep(Sleep sleep, SleepSegment segment) {
        sleep.updateSleep(
                segment.durationMinutes(),
                segment.bedTime(),
                segment.wakeUpTime(),
                segment.isNap(),
                segment.isAllNight(),
                segment.totalDurationMinutes(),
                segment.originalBedTime(),
                segment.originalWakeUpTime(),
                segment.continuousSleepGroupId()
        );
    }

    private List<SleepSegment> createSleepSegmentSpecs(
            int totalDurationMinutes,
            LocalDateTime originalBedTime,
            LocalDateTime originalWakeUpTime,
            Boolean isNap,
            Boolean isAllNight,
            String groupId
    ) {
        if (totalDurationMinutes <= MAX_SLEEP_DURATION_MINUTES) {
            return List.of(new SleepSegment(
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

        LocalDateTime segmentStart = originalBedTime;
        int remainingMinutes = totalDurationMinutes;
        List<SleepSegment> segments = new ArrayList<>();

        while (remainingMinutes > 0) {
            int segmentDurationMinutes = Math.min(remainingMinutes, MAX_SLEEP_DURATION_MINUTES);
            LocalDateTime segmentEnd = segmentStart.plusMinutes(segmentDurationMinutes);
            segments.add(new SleepSegment(
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

    private List<Sleep> orderedTargetSleeps(Long memberId, Sleep targetSleep) {
        List<Sleep> targetSleeps = new ArrayList<>(findTargetSleeps(memberId, targetSleep));
        targetSleeps.sort(Comparator.comparing(Sleep::getBedTime));
        targetSleeps.removeIf(sleep -> isSameSleep(sleep, targetSleep));
        targetSleeps.add(0, targetSleep);
        return targetSleeps;
    }

    private boolean isSameSleep(Sleep left, Sleep right) {
        if (left == right) {
            return true;
        }
        return left.getSleepId() != null && left.getSleepId().equals(right.getSleepId());
    }

    private String resolveContinuousSleepGroupId(Sleep targetSleep, int totalDurationMinutes) {
        if (totalDurationMinutes <= MAX_SLEEP_DURATION_MINUTES) {
            return null;
        }
        if (targetSleep.getContinuousSleepGroupId() != null) {
            return targetSleep.getContinuousSleepGroupId();
        }
        return UUID.randomUUID().toString();
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

    private record SleepSegment(
            int durationMinutes,
            LocalDateTime bedTime,
            LocalDateTime wakeUpTime,
            Boolean isNap,
            Boolean isAllNight,
            int totalDurationMinutes,
            LocalDateTime originalBedTime,
            LocalDateTime originalWakeUpTime,
            String continuousSleepGroupId
    ) {
    }
}
