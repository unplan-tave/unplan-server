package com.unplan.unplanserver.domain.onboarding.service;

import com.unplan.unplanserver.domain.onboarding.dto.request.BiorhythmRequest;
import com.unplan.unplanserver.domain.onboarding.dto.response.BiorhythmResponse;
import com.unplan.unplanserver.domain.onboarding.entity.Biorhythm;
import com.unplan.unplanserver.domain.onboarding.repository.BiorhythmRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BiorhythmService {

    private final BiorhythmRepository biorhythmRepository;

    @Transactional
    public BiorhythmResponse.UpdateBiorhythm updateBiorhythm(
            Long memberId,
            BiorhythmRequest request
    ) {
        validateSleepTimeline(request.getSleepTimeline());

        Biorhythm biorhythm = biorhythmRepository.findByMemberId(memberId)
                .orElseGet(() -> Biorhythm.builder()
                        .memberId(memberId)
                        .focusedTimeline(request.getFocusedTimeline())
                        .drowsyTimeline(request.getDrowsyTimeline())
                        .sleepTimeline(request.getSleepTimeline())
                        .build()
                );

        biorhythm.update(
                request.getFocusedTimeline(),
                request.getDrowsyTimeline(),
                request.getSleepTimeline()
        );

        Biorhythm savedBiorhythm = biorhythmRepository.save(biorhythm);

        return BiorhythmResponse.UpdateBiorhythm.from(savedBiorhythm);
    }

    public BiorhythmResponse.GetBiorhythm getBiorhythm(Long memberId) {
        return biorhythmRepository.findByMemberId(memberId)
                .map(BiorhythmResponse.GetBiorhythm::from)
                .orElseGet(() -> BiorhythmResponse.GetBiorhythm.defaultOf(memberId));
    }

    private void validateSleepTimeline(String sleepTimeline) {
        if (!sleepTimeline.contains("1")) {
            throw new CustomException(ErrorCode.SLEEP_REQUIRED);
        }

        if (!isCircularContinuous(sleepTimeline)) {
            throw new CustomException(ErrorCode.INVALID_SLEEP_PATTERN);
        }
    }

    private boolean isCircularContinuous(String timeline) {
        int startCount = 0;

        for (int i = 0; i < timeline.length(); i++) {
            char previous = timeline.charAt((i + 23) % 24);
            char current = timeline.charAt(i);

            if (previous == '0' && current == '1') {
                startCount++;
            }
        }

        return startCount == 1;
    }
}