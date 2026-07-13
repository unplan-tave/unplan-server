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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    void getSleepFallsBackToSleepFieldsWhenContinuousMetadataIsNull() {
        Long memberId = 1L;
        Long sleepId = 45L;
        LocalDateTime bedTime = LocalDateTime.of(2026, 6, 23, 23, 0);
        LocalDateTime wakeUpTime = LocalDateTime.of(2026, 6, 24, 7, 30);
        Sleep sleep = new Sleep(new Member(), 510, bedTime, wakeUpTime, false);
        ReflectionTestUtils.setField(sleep, "totalDurationMinutes", null);
        ReflectionTestUtils.setField(sleep, "originalBedTime", null);
        ReflectionTestUtils.setField(sleep, "originalWakeUpTime", null);

        when(sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId))
                .thenReturn(Optional.of(sleep));

        SleepResponse response = sleepService.getSleep(memberId, sleepId);

        assertThat(response.getTotalDurationMinutes()).isEqualTo(510);
        assertThat(response.getOriginalBedTime()).isEqualTo(bedTime);
        assertThat(response.getOriginalWakeUpTime()).isEqualTo(wakeUpTime);
    }

    @Test
    void getSleepReturnsContinuousSleepMetadata() {
        Long memberId = 1L;
        Long sleepId = 45L;
        LocalDateTime originalBedTime = LocalDateTime.of(2026, 6, 23, 23, 0);
        LocalDateTime originalWakeUpTime = LocalDateTime.of(2026, 6, 25, 7, 30);
        Sleep sleep = new Sleep(
                new Member(),
                510,
                LocalDateTime.of(2026, 6, 24, 23, 0),
                originalWakeUpTime,
                false,
                false,
                1950,
                originalBedTime,
                originalWakeUpTime,
                "group-1"
        );

        when(sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId))
                .thenReturn(Optional.of(sleep));

        SleepResponse response = sleepService.getSleep(memberId, sleepId);

        assertThat(response.getDurationMinutes()).isEqualTo(510);
        assertThat(response.getTotalDurationMinutes()).isEqualTo(1950);
        assertThat(response.getOriginalBedTime()).isEqualTo(originalBedTime);
        assertThat(response.getOriginalWakeUpTime()).isEqualTo(originalWakeUpTime);
        assertThat(response.getIsContinuousSleep()).isTrue();
        assertThat(response.getContinuousSleepGroupId()).isEqualTo("group-1");
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
        when(sleepRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SleepResponse response = sleepService.createSleep(memberId, request);

        assertThat(response.getDurationMinutes()).isEqualTo(600);
        assertThat(response.getTotalDurationMinutes()).isEqualTo(600);
        assertThat(response.getBedTime()).isEqualTo(request.bedTime());
        assertThat(response.getWakeUpTime()).isEqualTo(request.wakeUpTime());
        assertThat(response.getOriginalBedTime()).isEqualTo(request.bedTime());
        assertThat(response.getOriginalWakeUpTime()).isEqualTo(request.wakeUpTime());
        assertThat(response.getIsNap()).isFalse();
        assertThat(response.getIsAllNight()).isFalse();
        assertThat(response.getIsContinuousSleep()).isFalse();
        assertThat(response.getContinuousSleepGroupId()).isNull();
    }

    @Test
    void createSleepStoresZeroDurationWhenAllNightIsTrue() {
        Long memberId = 1L;
        Member member = new Member();
        LocalDateTime now = LocalDateTime.now();
        SleepRequest.SleepCreate request = new SleepRequest.SleepCreate(
                now.minusHours(24),
                now.minusMinutes(1),
                false,
                true
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(sleepRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SleepResponse response = sleepService.createSleep(memberId, request);

        assertThat(response.getDurationMinutes()).isZero();
        assertThat(response.getIsNap()).isFalse();
        assertThat(response.getIsAllNight()).isTrue();
    }

    @Test
    void createSleepSplitsContinuousSleepByTwentyFourHours() {
        Long memberId = 1L;
        Member member = new Member();
        LocalDateTime bedTime = LocalDateTime.of(2026, 6, 23, 23, 0);
        LocalDateTime wakeUpTime = LocalDateTime.of(2026, 6, 25, 7, 30);
        SleepRequest.SleepCreate request = new SleepRequest.SleepCreate(
                bedTime,
                wakeUpTime,
                false,
                false
        );

        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(
                member,
                request.bedTime(),
                request.wakeUpTime()
        )).thenReturn(false);
        when(sleepRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SleepResponse response = sleepService.createSleep(memberId, request);

        ArgumentCaptor<List<Sleep>> captor = ArgumentCaptor.forClass(List.class);
        verify(sleepRepository).saveAll(captor.capture());
        List<Sleep> savedSleeps = captor.getValue();

        assertThat(savedSleeps).hasSize(2);
        assertThat(savedSleeps.get(0).getDurationMinutes()).isEqualTo(1440);
        assertThat(savedSleeps.get(0).getBedTime()).isEqualTo(bedTime);
        assertThat(savedSleeps.get(0).getWakeUpTime()).isEqualTo(LocalDateTime.of(2026, 6, 24, 23, 0));
        assertThat(savedSleeps.get(1).getDurationMinutes()).isEqualTo(510);
        assertThat(savedSleeps.get(1).getBedTime()).isEqualTo(LocalDateTime.of(2026, 6, 24, 23, 0));
        assertThat(savedSleeps.get(1).getWakeUpTime()).isEqualTo(wakeUpTime);
        assertThat(savedSleeps.get(0).getContinuousSleepGroupId()).isNotBlank();
        assertThat(savedSleeps.get(1).getContinuousSleepGroupId()).isEqualTo(savedSleeps.get(0).getContinuousSleepGroupId());
        assertThat(response.getDurationMinutes()).isEqualTo(1440);
        assertThat(response.getTotalDurationMinutes()).isEqualTo(1950);
        assertThat(response.getOriginalBedTime()).isEqualTo(bedTime);
        assertThat(response.getOriginalWakeUpTime()).isEqualTo(wakeUpTime);
        assertThat(response.getIsContinuousSleep()).isTrue();
        assertThat(response.getContinuousSleepGroupId()).isEqualTo(savedSleeps.get(0).getContinuousSleepGroupId());
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
    void updateContinuousSleepPreservesRequestedSleepIdAndDeletesOnlyUnusedSegments() {
        Long memberId = 1L;
        Long sleepId = 45L;
        Member member = new Member();
        Sleep firstSegment = new Sleep(
                member,
                1440,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 24, 23, 0),
                false,
                false,
                1950,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 25, 7, 30),
                "group-1"
        );
        Sleep secondSegment = new Sleep(
                member,
                510,
                LocalDateTime.of(2026, 6, 24, 23, 0),
                LocalDateTime.of(2026, 6, 25, 7, 30),
                false,
                false,
                1950,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 25, 7, 30),
                "group-1"
        );
        setSleepId(firstSegment, sleepId);
        setSleepId(secondSegment, 46L);
        SleepRequest.SleepUpdate request = new SleepRequest.SleepUpdate(
                LocalDateTime.of(2026, 6, 23, 22, 0),
                LocalDateTime.of(2026, 6, 24, 6, 0),
                false,
                false
        );

        when(sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId))
                .thenReturn(Optional.of(firstSegment));
        when(sleepRepository.findAllByMemberMemberIdAndContinuousSleepGroupId(memberId, "group-1"))
                .thenReturn(List.of(firstSegment, secondSegment));
        when(conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(
                member,
                request.bedTime(),
                request.wakeUpTime()
        )).thenReturn(false);

        SleepResponse response = sleepService.updateSleep(memberId, sleepId, request);

        verify(sleepRepository).deleteAll(List.of(secondSegment));
        assertThat(response.getSleepId()).isEqualTo(sleepId);
        assertThat(response.getDurationMinutes()).isEqualTo(480);
        assertThat(response.getIsContinuousSleep()).isFalse();
        assertThat(response.getContinuousSleepGroupId()).isNull();
        assertThat(firstSegment.getSleepId()).isEqualTo(sleepId);
        assertThat(firstSegment.getDurationMinutes()).isEqualTo(480);
        assertThat(firstSegment.getContinuousSleepGroupId()).isNull();
    }

    @Test
    void getSleepReturnsUpdatedContinuousSleepAfterPatchWithoutNotFound() {
        Long memberId = 1L;
        Long sleepId = 45L;
        Member member = new Member();
        Sleep firstSegment = new Sleep(
                member,
                1440,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 24, 23, 0),
                false,
                false,
                1950,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 25, 7, 30),
                "group-1"
        );
        Sleep secondSegment = new Sleep(
                member,
                510,
                LocalDateTime.of(2026, 6, 24, 23, 0),
                LocalDateTime.of(2026, 6, 25, 7, 30),
                false,
                false,
                1950,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 25, 7, 30),
                "group-1"
        );
        setSleepId(firstSegment, sleepId);
        setSleepId(secondSegment, 46L);
        SleepRequest.SleepUpdate request = new SleepRequest.SleepUpdate(
                LocalDateTime.of(2026, 6, 23, 22, 0),
                LocalDateTime.of(2026, 6, 24, 6, 0),
                false,
                false
        );

        when(sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId))
                .thenReturn(Optional.of(firstSegment));
        when(sleepRepository.findAllByMemberMemberIdAndContinuousSleepGroupId(memberId, "group-1"))
                .thenReturn(List.of(firstSegment, secondSegment));
        when(conditionRepository.existsByMemberAndMeasuredAtAfterAndMeasuredAtBefore(
                member,
                request.bedTime(),
                request.wakeUpTime()
        )).thenReturn(false);

        SleepResponse patchResponse = sleepService.updateSleep(memberId, sleepId, request);
        SleepResponse getResponse = sleepService.getSleep(memberId, sleepId);

        assertThat(patchResponse.getSleepId()).isEqualTo(sleepId);
        assertThat(getResponse.getSleepId()).isEqualTo(sleepId);
        assertThat(getResponse.getDurationMinutes()).isEqualTo(480);
        assertThat(getResponse.getWakeUpTime()).isEqualTo(request.wakeUpTime());
    }

    @Test
    void updateSingleSleepToContinuousSleepPreservesRequestedSleepIdAndCreatesOnlyAdditionalSegments() {
        Long memberId = 1L;
        Long sleepId = 45L;
        Member member = new Member();
        Sleep sleep = new Sleep(
                member,
                480,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 24, 7, 0),
                false
        );
        setSleepId(sleep, sleepId);
        SleepRequest.SleepUpdate request = new SleepRequest.SleepUpdate(
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 25, 7, 30),
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
        when(sleepRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        SleepResponse response = sleepService.updateSleep(memberId, sleepId, request);

        ArgumentCaptor<List<Sleep>> captor = ArgumentCaptor.forClass(List.class);
        verify(sleepRepository).saveAll(captor.capture());
        List<Sleep> addedSegments = captor.getValue();

        assertThat(response.getSleepId()).isEqualTo(sleepId);
        assertThat(response.getDurationMinutes()).isEqualTo(1440);
        assertThat(response.getTotalDurationMinutes()).isEqualTo(1950);
        assertThat(response.getOriginalBedTime()).isEqualTo(request.bedTime());
        assertThat(response.getOriginalWakeUpTime()).isEqualTo(request.wakeUpTime());
        assertThat(response.getIsContinuousSleep()).isTrue();
        assertThat(response.getContinuousSleepGroupId()).isNotBlank();
        assertThat(sleep.getSleepId()).isEqualTo(sleepId);
        assertThat(addedSegments).hasSize(1);
        assertThat(addedSegments.get(0).getDurationMinutes()).isEqualTo(510);
        assertThat(addedSegments.get(0).getContinuousSleepGroupId()).isEqualTo(response.getContinuousSleepGroupId());
        assertThat(addedSegments.get(0).getOriginalBedTime()).isEqualTo(request.bedTime());
        assertThat(addedSegments.get(0).getOriginalWakeUpTime()).isEqualTo(request.wakeUpTime());
        assertThat(addedSegments.get(0).getTotalDurationMinutes()).isEqualTo(1950);
    }

    @Test
    void deleteSleepDeletesAllContinuousSleepSegments() {
        Long memberId = 1L;
        Long sleepId = 45L;
        Member member = new Member();
        Sleep firstSegment = new Sleep(
                member,
                1440,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 24, 23, 0),
                false,
                false,
                1950,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 25, 7, 30),
                "group-1"
        );
        Sleep secondSegment = new Sleep(
                member,
                510,
                LocalDateTime.of(2026, 6, 24, 23, 0),
                LocalDateTime.of(2026, 6, 25, 7, 30),
                false,
                false,
                1950,
                LocalDateTime.of(2026, 6, 23, 23, 0),
                LocalDateTime.of(2026, 6, 25, 7, 30),
                "group-1"
        );

        when(sleepRepository.findBySleepIdAndMemberMemberId(sleepId, memberId))
                .thenReturn(Optional.of(firstSegment));
        when(sleepRepository.findAllByMemberMemberIdAndContinuousSleepGroupId(memberId, "group-1"))
                .thenReturn(List.of(firstSegment, secondSegment));

        sleepService.deleteSleep(memberId, sleepId);

        verify(sleepRepository).deleteAll(List.of(firstSegment, secondSegment));
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

    private void setSleepId(Sleep sleep, Long sleepId) {
        ReflectionTestUtils.setField(sleep, "sleepId", sleepId);
    }
}
