package com.unplan.unplanserver.domain.onboarding.dto.response;

import com.unplan.unplanserver.domain.onboarding.entity.Biorhythm;

public class BiorhythmResponse {

    private static final String DEFAULT_TIMELINE = "000000000000000000000000";

    public record UpdateBiorhythm(
            Long memberId,
            String focusedTimeline,
            String drowsyTimeline,
            String sleepTimeline
    ) {
        public static UpdateBiorhythm from(Biorhythm biorhythm) {
            return new UpdateBiorhythm(
                    biorhythm.getMemberId(),
                    biorhythm.getFocusedTimeline(),
                    biorhythm.getDrowsyTimeline(),
                    biorhythm.getSleepTimeline()
            );
        }
    }

    public record GetBiorhythm(
            Long memberId,
            String focusedTimeline,
            String drowsyTimeline,
            String sleepTimeline
    ) {
        public static GetBiorhythm from(Biorhythm biorhythm) {
            return new GetBiorhythm(
                    biorhythm.getMemberId(),
                    biorhythm.getFocusedTimeline(),
                    biorhythm.getDrowsyTimeline(),
                    biorhythm.getSleepTimeline()
            );
        }

        public static GetBiorhythm defaultOf(Long memberId) {
            return new GetBiorhythm(
                    memberId,
                    DEFAULT_TIMELINE,
                    DEFAULT_TIMELINE,
                    DEFAULT_TIMELINE
            );
        }
    }
}