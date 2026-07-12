package com.unplan.unplanserver.domain.recommendation.controller;

import com.unplan.unplanserver.domain.recommendation.dto.response.QueueCardRecommendationResult;
import com.unplan.unplanserver.domain.recommendation.service.RecommendationService;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Recommendation", description = "큐 카드 → 핀 카드 전환용 7일 추천 시간대 API")
@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class QueueCardRecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "큐 카드 추천 시간대 조회",
            description = "특정 큐 카드를 핀 카드로 전환할 후보 시간대를 오늘부터 days일 이내에서 날짜별 1개씩 찾습니다. "
                    + "후보가 없으면 409로 { canExtendTo14Days, mustChangeDuration } 를 반환합니다. "
                    + "각 후보의 recommendId 로 기존 수락 API(POST /schedule/recommendations/{recommendId}/accept)를 호출해 핀 전환합니다.")
    @GetMapping("/{scheduleId}/recommendations")
    public ResponseEntity<?> getQueueCardRecommendations(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "추천받을 큐 카드 id", example = "1")
            @PathVariable Long scheduleId,
            @Parameter(description = "탐색 범위(일). 7(기본) 또는 14(확장)", example = "7")
            @RequestParam(defaultValue = "7") int days) {

        if (days != 7 && days != 14) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        QueueCardRecommendationResult result =
                recommendationService.getQueueCardRecommendations(memberId, scheduleId, days);

        return result.hasSlots()
                ? ResponseEntity.ok(result.success())
                : ResponseEntity.status(HttpStatus.CONFLICT).body(result.noSlot());
    }
}
