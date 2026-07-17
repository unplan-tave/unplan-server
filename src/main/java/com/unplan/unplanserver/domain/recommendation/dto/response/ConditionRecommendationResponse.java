package com.unplan.unplanserver.domain.recommendation.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ConditionRecommendationResponse(
        LocalDate date,
        String resultType,
        String conditionTag,
        String conditionTagLabel,
        EmptyTime emptyTime,
        String summaryMessage,
        List<SummaryTag> summaryTags,
        List<RecommendationItem> recommendations
) {

    public record EmptyTime(
            LocalTime startTime,
            LocalTime endTime,
            int durationMinutes
    ) {
    }

    public record SummaryTag(
            String tag,
            String label
    ) {
    }

    public record RecommendationItem(
            Long recommendId,
            Long sourceScheduleId,
            String title,
            LocalTime startTime,
            LocalTime endTime,
            Integer estimatedTime,
            LocalDate deadline,
            String conditionTag,
            String conditionTagLabel,
            String sourceType,
            String matchTier,
            int displayOrder,
            String suitabilityMessage,
            String timeMarginMessage,
            List<String> recoveryMeans
    ) {
    }
}
