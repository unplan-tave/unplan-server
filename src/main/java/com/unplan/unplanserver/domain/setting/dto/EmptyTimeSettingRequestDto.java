package com.unplan.unplanserver.domain.setting.dto;


import java.time.*;
import java.util.*;

public record EmptyTimeSettingRequestDto(
        Boolean isEmptyTimeRecommendOn,
        Integer emptyTimeCriteriaMinutes,
        Boolean isRecommendBanTimeOn,
        List<RecommendBanTime> recommendBanTimes
) {
    public record RecommendBanTime(
            LocalTime startTime,
            LocalTime endTime
    ) {
    }
}
