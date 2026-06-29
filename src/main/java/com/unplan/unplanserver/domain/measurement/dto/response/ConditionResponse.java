package com.unplan.unplanserver.domain.measurement.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.unplan.unplanserver.domain.measurement.entity.Condition;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ConditionResponse {

    @JsonProperty("conditionId")
    private Long conditionId;

    @JsonProperty("bodyScore")
    private Integer bodyScore;

    @JsonProperty("mindScore")
    private Integer mindScore;

    @JsonProperty("dateTime")
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
