package com.unplan.unplanserver.domain.measurement.service;

import com.unplan.unplanserver.domain.measurement.dto.request.SleepRequest;
import com.unplan.unplanserver.domain.measurement.dto.response.SleepResponse;
import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import com.unplan.unplanserver.domain.measurement.repository.SleepRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SleepService {

    private final SleepRepository sleepRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public SleepResponse createSleep(Long memberId, SleepRequest.SleepCreate request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        LocalDateTime bedTime = request.wakeUpTime()
                .minusMinutes(request.durationMinutes());

        Boolean isNap = request.durationMinutes() <= 180 && request.isNap();

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
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Sleep sleep = sleepRepository.findBySleepIdAndMember(sleepId, member)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수면 기록입니다."));

        LocalDateTime bedTime = request.wakeUpTime()
                .minusMinutes(request.durationMinutes());

        Boolean isNap = request.durationMinutes() <= 180 && request.isNap();

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

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Sleep sleep = sleepRepository.findBySleepIdAndMember(sleepId, member)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 수면 기록입니다."));

        sleepRepository.delete(sleep);

        // TODO: 수면 삭제 후 당일 수면 부족 패널티 및 종합 컨디션 점수 재계산 로직 연결
    }
}