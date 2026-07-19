package com.unplan.unplanserver.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

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
        @Schema(description = "추천 일정 목록")
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

    @Schema(name = "ConditionRecommendationItem", description = "컨디션 기반 추천 일정")
    public record RecommendationItem(
            Long recommendId,
            @Schema(description = "추천 원본 일정 ID", example = "10")
            Long sourceScheduleId,
            String title,
            LocalTime startTime,
            LocalTime endTime,
            Integer estimatedTime,
            LocalDate deadline,
            String conditionTag,
            @Schema(description = "컨디션 태그 표시명", example = "핵심 작업")
            String conditionTagLabel,
            String sourceType,
            String matchTier,
            int displayOrder,
            @Schema(
                    description = "컨디션 적합도 기반 추천 이유 문구",
                    example = "부담 없이 가볍게 시작하기 좋은 상태예요"
            )
            String suitabilityMessage,
            @Schema(
                    description = "빈 시간과 예상 소요 시간 적합도 기반 추천 이유 문구",
                    example = "일정이 2배 이상 길어져도 시간 여유가 괜찮아요"
            )
            String timeMarginMessage,
            List<String> recoveryMeans
    ) {
    }
}
