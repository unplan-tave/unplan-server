package com.unplan.unplanserver.domain.setting.dto;

import com.unplan.unplanserver.domain.setting.entity.RecommendBanTime;

import java.time.LocalTime;
import java.util.List;

public record EmptyTimeSettingResponseDto (
        Boolean isEmptyTimeRecommendOn,
        Integer emptyTimeCriteriaMinutes,
        Boolean isRecommendBanTimeOn,
        List<RecommendBanTime> recommendBanTimes
){
    public record RecommendBanTime(
            LocalTime startTime,
            LocalTime endTime
    ){
    }
}
