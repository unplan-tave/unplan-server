package com.unplan.unplanserver.domain.measurement.controller;

import com.unplan.unplanserver.domain.measurement.dto.request.ConditionRequest;
import com.unplan.unplanserver.domain.measurement.dto.response.ConditionResponse;
import com.unplan.unplanserver.domain.measurement.service.ConditionService;
import com.unplan.unplanserver.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Condition", description = "컨디션 기록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/conditions")
public class ConditionController {

    private final ConditionService conditionService;

    @Operation(
            summary = "컨디션 입력",
            description = "신체(Energy) 점수와 정신(Focus) 점수를 한 세트로 기록합니다. 점수는 각각 0~6 사이로 입력합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ConditionResponse>> createCondition(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ConditionRequest.ConditionCreate request
    ) {

        ConditionResponse response =
                conditionService.createCondition(memberId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(
            summary = "컨디션 수정",
            description = "기존에 입력한 신체(Energy) 점수와 정신(Focus) 점수를 수정합니다. 점수는 각각 0~6 사이로 입력합니다."
    )
    @PatchMapping("/{conditionId}")
    public ResponseEntity<ApiResponse<ConditionResponse>> updateCondition(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long conditionId,
            @Valid @RequestBody ConditionRequest.ConditionUpdate request
    ) {

        ConditionResponse response =
                conditionService.updateCondition(memberId, conditionId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "컨디션 삭제",
            description = "기존에 입력한 컨디션 기록 세트를 삭제합니다."
    )
    @DeleteMapping("/{conditionId}")
    public ResponseEntity<ApiResponse<Void>> deleteCondition(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long conditionId
    ) {

        conditionService.deleteCondition(memberId, conditionId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}