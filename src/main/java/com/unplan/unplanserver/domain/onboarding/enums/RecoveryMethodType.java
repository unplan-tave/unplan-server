package com.unplan.unplanserver.domain.onboarding.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecoveryMethodType {
    NAP("짧은 낮잠"),
    MUSIC("음악 감상"),
    WALK("가벼운 산책"),
    STRETCHING("스트레칭"),
    FOOD("음식/간식");

    private final String description;
}