package com.unplan.unplanserver.domain.recommendation.controller;

import com.unplan.unplanserver.domain.recommendation.dto.request.RecommendationAcceptRequest;
import com.unplan.unplanserver.domain.recommendation.dto.response.ConditionRecommendationResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationAcceptResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse;
import com.unplan.unplanserver.domain.recommendation.service.RecommendationService;
import com.unplan.unplanserver.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "Recommendation", description = "홈/컨디션 탭 일정 추천 API")
@RestController
@RequestMapping("/schedule/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "컨디션 기반 추천 일정 조회",
            description = "컨디션 탭 바텀시트에 노출할 빈 시간, 컨디션 태그, 추천 일정, 추천 문구를 조회합니다.")
    @GetMapping("/condition")
    public ResponseEntity<ApiResponse<ConditionRecommendationResponse>> getConditionRecommendations(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "추천 대상 날짜 (yyyy-MM-dd)", example = "2026-05-06")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(ApiResponse.success(
                recommendationService.getConditionRecommendations(memberId, date)
        ));
    }

    @Operation(summary = "추천 목록 조회",
            description = "빈 시간에 배치된 큐 카드 추천 목록을 조회합니다. 조회 시마다 현재 컨디션·큐 카드·핀 카드 기준으로 재생성됩니다.")
    @GetMapping
    public ResponseEntity<RecommendationListResponse> getRecommendations(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "추천 대상 날짜 (yyyy-MM-dd)", example = "2026-07-04")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(recommendationService.getRecommendations(memberId, date));
    }

    @Operation(summary = "추천 수락",
            description = "추천을 수락합니다. 큐 카드 추천은 원본 큐 카드를 핀 카드로 전환하고, 회복 수단 추천은 새 일정을 생성합니다. "
                    + "'기존 큐 카드 유지하기'(keepQueueCard=true) 시 큐 카드를 남긴 채 핀 카드를 복제 생성합니다.")
    @PostMapping("/{recommendId}/accept")
    public ResponseEntity<RecommendationAcceptResponse> acceptRecommendation(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "수락할 추천 ID", example = "1")
            @PathVariable Long recommendId,
            @RequestBody(required = false) RecommendationAcceptRequest request) {

        boolean keepQueueCard = request != null && request.keepQueueCardOrDefault();
        String recoveryMean = request != null ? request.recoveryMean() : null;
        return ResponseEntity.ok(recommendationService.accept(memberId, recommendId, keepQueueCard, recoveryMean));
    }

    @Operation(summary = "추천 패스",
            description = "추천을 패스(넘기기)합니다. 삭제가 아니라 해당 추천의 원본 큐 카드를 그날 추천에서만 제외하며, "
                    + "다음 날 같은 큐 카드는 다시 추천 후보가 됩니다. 회복 수단 추천은 패스할 수 없습니다(400).")
    @PostMapping("/{recommendId}/pass")
    public ResponseEntity<Void> passRecommendation(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "패스할 추천 ID", example = "1")
            @PathVariable Long recommendId) {

        recommendationService.pass(memberId, recommendId);
        return ResponseEntity.noContent().build();
    }
}
