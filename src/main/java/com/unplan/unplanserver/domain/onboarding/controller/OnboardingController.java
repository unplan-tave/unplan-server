package com.unplan.unplanserver.domain.onboarding.controller;

import com.unplan.unplanserver.domain.onboarding.dto.request.OnboardingRequest;
import com.unplan.unplanserver.domain.onboarding.dto.response.OnboardingResponse;
import com.unplan.unplanserver.domain.onboarding.service.OnboardingService;
import com.unplan.unplanserver.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Onboarding", description = "최초 온보딩 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    @Operation(
            summary = "최초 온보딩 저장",
            description = "회복 방법, 수면 컨디션, 하루 활동 패턴, 선호 이동 방식을 한 번에 저장합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<OnboardingResponse>> saveOnboarding(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody OnboardingRequest request
    ) {
        OnboardingResponse response =
                onboardingService.saveOnboarding(memberId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}