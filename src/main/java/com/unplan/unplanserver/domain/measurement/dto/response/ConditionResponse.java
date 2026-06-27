package com.unplan.unplanserver.domain.measurement.dto.response;

import com.unplan.unplanserver.domain.measurement.entity.Condition;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConditionResponse {

    private Long conditionId;
    private Integer bodyScore;
    private Integer mindScore;
    private LocalDateTime dateTime;

    public static ConditionResponse from(Condition condition) {
        return ConditionResponse.builder()
                .conditionId(condition.getConditionId())
                .bodyScore(condition.getBodyScore())
                .mindScore(condition.getMindScore())
                .dateTime(condition.getMeasuredAt())
                .build();
    }
}