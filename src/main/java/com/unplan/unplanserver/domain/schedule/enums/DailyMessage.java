package com.unplan.unplanserver.domain.schedule.enums;

import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
@Getter
@RequiredArgsConstructor
public enum DailyMessage {
    CORE_TASK("핵심 작업", ConditionTag.CORE_TASK,"몸과 머리가 모두 좋은 상태예요", "오늘 가장 중요한 일을 시작해보세요"),
    BRAIN_WORK("두뇌 활동", ConditionTag.BRAIN_WORK, "집중력이 높은 상태예요", "생각이 필요한 일을 해보세요"),
    SIMPLE_TASK("단순 노동", ConditionTag.SIMPLE_TASK, "몸은 괜찮지만 머리가 무거운 상태예요","단순한 일부터 시작해보세요"),
    DAILY_TASK("일상 작업", ConditionTag.DAILY_TASK, "컨디션이 안정적인 편이에요", "루틴한 일정을 이어가보세요"),
    URGENT("긴급 처리", ConditionTag.URGENT, "에너지가 부족한 상태예요","꼭 필요한 일만 처리해보세요"),
    RECOVERY("기력 회복", ConditionTag.RECOVERY, "빠르게 회복이 필요한 상태예요", "회복에 집중해보세요"),
    ;
    private final String label;
    private final ConditionTag conditionTag;
    private final String prescription;
    private final String suggestion;

    public static DailyMessage fromLabel(String label) {
        return Arrays.stream(values())
                .filter(tag->tag.label.equals(label))
                .findFirst()
                .orElseThrow(()->new CustomException(ErrorCode.CONDITION_TAG_NOT_FOUND));
    }
}
