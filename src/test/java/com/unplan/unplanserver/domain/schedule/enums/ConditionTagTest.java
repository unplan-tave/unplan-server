package com.unplan.unplanserver.domain.schedule.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConditionTagTest {

    @Test
    void fromLabelMapsEveryKoreanLabel() {
        assertThat(ConditionTag.fromLabel("핵심 작업")).isEqualTo(ConditionTag.CORE_TASK);
        assertThat(ConditionTag.fromLabel("두뇌 활동")).isEqualTo(ConditionTag.BRAIN_WORK);
        assertThat(ConditionTag.fromLabel("단순 노동")).isEqualTo(ConditionTag.SIMPLE_TASK);
        assertThat(ConditionTag.fromLabel("일상 작업")).isEqualTo(ConditionTag.DAILY_TASK);
        assertThat(ConditionTag.fromLabel("긴급 처리")).isEqualTo(ConditionTag.URGENT);
        assertThat(ConditionTag.fromLabel("기력 회복")).isEqualTo(ConditionTag.RECOVERY);
    }

    @Test
    void fromLabelRoundTripsWithGetLabel() {
        for (ConditionTag tag : ConditionTag.values()) {
            assertThat(ConditionTag.fromLabel(tag.getLabel())).isEqualTo(tag);
        }
    }

    @Test
    void fromLabelRejectsUnknownLabel() {
        assertThatThrownBy(() -> ConditionTag.fromLabel("없는 태그"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("없는 태그");
    }
}
