package com.unplan.unplanserver.domain.onboarding.controller;

import com.unplan.unplanserver.domain.onboarding.dto.request.RecoverRequest;
import com.unplan.unplanserver.domain.onboarding.dto.request.SleepConditionRequest;
import com.unplan.unplanserver.domain.onboarding.dto.response.RecoverResponse;
import com.unplan.unplanserver.domain.onboarding.dto.response.SleepConditionResponse;
import com.unplan.unplanserver.domain.onboarding.service.RecoverService;
import com.unplan.unplanserver.domain.onboarding.service.SleepConditionService;
import com.unplan.unplanserver.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Onboarding", description = "온보딩 설정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member-settings")
public class OnboardingController {

    private final RecoverService recoverService;
    private final SleepConditionService sleepConditionService;

    @Operation(
            summary = "컨디션 회복 방법 설정 저장 및 전체 수정",
            description = "처음 온보딩 입력 시에도 사용 가능하며, 기존 설정이 있으면 요청값으로 전체 교체합니다.<br>" +
                    "defaultMethods에는 NAP, MUSIC, WALK, STRETCHING, FOOD 중 선택한 값을 넣어 주세요.<br>" +
                    "직접 입력값이 없으면 customMethods는 빈 배열 []로 보내 주세요."
    )
    @PutMapping("/recovery-methods")
    public ResponseEntity<ApiResponse<RecoverResponse.UpdateMethods>> updateRecoveryMethods(
            // @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RecoverRequest.UpdateMethods request
    ) {
        Long memberId = 1L;

        RecoverResponse.UpdateMethods response =
                recoverService.updateMethods(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "컨디션 회복 방법 조회",
            description = "회원이 설정한 기본 회복 방법과 직접 입력한 회복 방법을 조회합니다."
    )
    @GetMapping("/recovery-methods")
    public ResponseEntity<ApiResponse<RecoverResponse.GetMethods>> getRecoveryMethods(
            // @AuthenticationPrincipal Long memberId
    ) {
        Long memberId = 1L;

        RecoverResponse.GetMethods response = recoverService.getMethods(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "수면 시간별 컨디션 설정 저장 및 수정",
            description = "수면 시간별 컨디션 기준을 설정합니다. 모든 값은 30분 단위이며, 과다 기준값은 720분으로 고정됩니다."
    )
    @PutMapping("/sleep-conditions")
    public ResponseEntity<ApiResponse<SleepConditionResponse.UpdateConditions>> updateSleepConditions(
            // @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SleepConditionRequest.UpdateConditions request
    ) {
        Long memberId = 1L;

        SleepConditionResponse.UpdateConditions response =
                sleepConditionService.updateSleepCondition(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "수면 시간별 컨디션 조회",
            description = "회원이 설정한 수면 시간별 컨디션 기준을 조회합니다."
    )
    @GetMapping("/sleep-conditions")
    public ResponseEntity<ApiResponse<SleepConditionResponse.GetConditions>> getSleepConditions(
            // @AuthenticationPrincipal Long memberId
    ) {
        Long memberId = 1L;

        SleepConditionResponse.GetConditions response =
                sleepConditionService.getSleepCondition(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}