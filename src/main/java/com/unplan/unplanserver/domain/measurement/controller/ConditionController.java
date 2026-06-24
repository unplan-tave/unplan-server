package com.unplan.unplanserver.domain.measurement.controller;

import com.unplan.unplanserver.domain.measurement.dto.request.ConditionCreateRequest;
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
            description = "ENERGY 또는 FOCUS 컨디션 점수를 기록합니다. 점수는 0~6 사이로 입력합니다."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ConditionResponse>> createCondition(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ConditionCreateRequest request
    ) {

        ConditionResponse response =
                conditionService.createCondition(memberId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}