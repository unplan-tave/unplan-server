package com.unplan.unplanserver.domain.onboarding.controller;

import com.unplan.unplanserver.domain.onboarding.dto.request.BiorhythmRequest;
import com.unplan.unplanserver.domain.onboarding.dto.request.RecoverRequest;
import com.unplan.unplanserver.domain.onboarding.dto.request.SleepConditionRequest;
import com.unplan.unplanserver.domain.onboarding.dto.request.TransportRequest;
import com.unplan.unplanserver.domain.onboarding.dto.response.BiorhythmResponse;
import com.unplan.unplanserver.domain.onboarding.dto.response.RecoverResponse;
import com.unplan.unplanserver.domain.onboarding.dto.response.SleepConditionResponse;
import com.unplan.unplanserver.domain.onboarding.dto.response.TransportResponse;
import com.unplan.unplanserver.domain.onboarding.service.BiorhythmService;
import com.unplan.unplanserver.domain.onboarding.service.RecoverService;
import com.unplan.unplanserver.domain.onboarding.service.SleepConditionService;
import com.unplan.unplanserver.domain.onboarding.service.TransportService;
import com.unplan.unplanserver.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Onboarding", description = "온보딩 설정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/member-settings")
public class OnboardingController {

    private final RecoverService recoverService;
    private final SleepConditionService sleepConditionService;
    private final BiorhythmService biorhythmService;
    private final TransportService transportService;

    @Operation(
            summary = "컨디션 회복 방법 설정 저장 및 전체 수정",
            description = "처음 온보딩 입력 시에도 사용 가능하며, 기존 설정이 있으면 요청값으로 전체 교체합니다.<br>" +
                    "defaultMethods에는 NAP, MUSIC, WALK, STRETCHING, FOOD 중 선택한 값을 넣어 주세요.<br>" +
                    "직접 입력값이 없으면 customMethods는 빈 배열 []로 보내 주세요."
    )
    @PutMapping("/recovery-methods")
    public ResponseEntity<ApiResponse<RecoverResponse.UpdateMethods>> updateRecoveryMethods(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody RecoverRequest.UpdateMethods request
    ) {

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
            @AuthenticationPrincipal Long memberId
    ) {

        RecoverResponse.GetMethods response = recoverService.getMethods(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "수면 시간별 컨디션 설정 저장 및 수정",
            description = "수면 시간별 컨디션 기준을 설정합니다. 모든 값은 30분 단위이며, 과다 기준값은 720분으로 고정됩니다."
    )
    @PutMapping("/sleep-conditions")
    public ResponseEntity<ApiResponse<SleepConditionResponse>> updateSleepConditions(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SleepConditionRequest.UpdateConditions request
    ) {

        SleepConditionResponse response =
                sleepConditionService.updateSleepCondition(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "수면 시간별 컨디션 조회",
            description = "회원이 설정한 수면 시간별 컨디션 기준을 조회합니다."
    )
    @GetMapping("/sleep-conditions")
    public ResponseEntity<ApiResponse<SleepConditionResponse>> getSleepConditions(
            @AuthenticationPrincipal Long memberId
    ) {

        SleepConditionResponse response =
                sleepConditionService.getSleepCondition(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "하루 활동 패턴 설정 저장 및 수정",
            description = "집중 잘 되는 시간, 졸린 시간, 수면 시간을 24자리 문자열로 설정합니다. 수면 시간은 최소 한 칸 이상 필수이며, 중간에 끊길 수 없습니다."
    )
    @PutMapping("/biorhythms")
    public ResponseEntity<ApiResponse<BiorhythmResponse.UpdateBiorhythm>> updateBiorhythm(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody BiorhythmRequest request
    ) {

        BiorhythmResponse.UpdateBiorhythm response =
                biorhythmService.updateBiorhythm(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "하루 활동 패턴 설정 조회",
            description = "회원이 설정한 하루 활동 패턴을 조회합니다. 저장값이 없으면 000000000000000000000000으로 반환합니다."
    )
    @GetMapping("/biorhythms")
    public ResponseEntity<ApiResponse<BiorhythmResponse.GetBiorhythm>> getBiorhythm(
            @AuthenticationPrincipal Long memberId
    ) {

        BiorhythmResponse.GetBiorhythm response =
                biorhythmService.getBiorhythm(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "선호 이동 방식 설정 저장 및 수정",
            description = "선호하는 이동 방식을 설정합니다.<br>" +
                    "transportTypes에는 WALK, BICYCLE, PUBLIC_TRANSPORT, CAR 중 선택한 값을 넣어 주세요.<br>" +
                    "아무것도 선택하지 않은 경우 transportTypes는 빈 배열 []로 보내 주세요."
    )
    @PutMapping("/transportations")
    public ResponseEntity<ApiResponse<TransportResponse>> updateTransport(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody TransportRequest request
    ) {

        TransportResponse response =
                transportService.updateTransport(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "선호 이동 방식 조회",
            description = "회원이 설정한 선호 이동 방식을 조회합니다. 저장값이 없으면 transportTypes는 빈 배열 []로 반환합니다."
    )
    @GetMapping("/transportations")
    public ResponseEntity<ApiResponse<TransportResponse>> getTransport(
            @AuthenticationPrincipal Long memberId
    ) {

        TransportResponse response =
                transportService.getTransport(memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}