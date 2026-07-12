package com.unplan.unplanserver.domain.schedule.enums;

import java.util.Arrays;

public enum ConditionTag {
    CORE_TASK("핵심 작업"),
    BRAIN_WORK("두뇌 활동"),
    SIMPLE_TASK("단순 노동"),
    DAILY_TASK("일상 작업"),
    URGENT("긴급 처리"),
    RECOVERY("기력 회복");

    private final String label;

    ConditionTag(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 컨디션 점수 모듈(ConditionScoreCalculator)이 산출하는 한글 태그 문자열을 enum 으로 변환한다.
     * 추천 엔진은 ConditionTag enum 을 입력으로 받으므로, 두 모듈을 잇는 브릿지 역할.
     */
    public static ConditionTag fromLabel(String label) {
        return Arrays.stream(values())
                .filter(tag -> tag.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 컨디션 태그: " + label));
    }
}
