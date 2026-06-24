package com.unplan.unplanserver.domain.measurement.dto.response;

import com.unplan.unplanserver.domain.measurement.entity.Condition;
import com.unplan.unplanserver.domain.measurement.enums.ConditionType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConditionResponse {

    private Long conditionId;
    private ConditionType conditionType;
    private Integer score;
    private LocalDateTime createdAt;

    public static ConditionResponse from(Condition condition) {
        return ConditionResponse.builder()
                .conditionId(condition.getConditionId())
                .conditionType(condition.getConditionType())
                .score(condition.getScore())
                .createdAt(condition.getCreatedAt())
                .build();
    }
}