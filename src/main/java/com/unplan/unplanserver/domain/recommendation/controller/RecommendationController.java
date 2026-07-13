package com.unplan.unplanserver.domain.recommendation.controller;

import com.unplan.unplanserver.domain.recommendation.dto.request.RecommendationAcceptRequest;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationAcceptResponse;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationListResponse;
import com.unplan.unplanserver.domain.recommendation.service.RecommendationService;
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
}
