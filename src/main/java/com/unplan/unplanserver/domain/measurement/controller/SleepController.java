package com.unplan.unplanserver.domain.measurement.controller;

import com.unplan.unplanserver.domain.measurement.dto.request.SleepRequest;
import com.unplan.unplanserver.domain.measurement.dto.response.SleepResponse;
import com.unplan.unplanserver.domain.measurement.service.SleepService;
import com.unplan.unplanserver.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Sleep", description = "수면 기록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/sleeps")
public class SleepController {

    private final SleepService sleepService;

    @Operation(
            summary = "수면 입력",
            description = """
                    사용자가 기록한 밤잠 또는 낮잠 세션 정보를 등록합니다.<br>
                    사용자는 총 수면 시간, 기상 시각, 낮잠 여부를 입력합니다.<br><br>
                    
                    - bed_time은 요청값으로 받지 않습니다.<br>
                    - 서버에서 wake_up_time - duration_minutes 값으로 bed_time을 자동 계산합니다.<br>
                    - duration_minutes가 180분을 초과하면 낮잠으로 저장되지 않습니다.<br>
                    - created_at은 서버에서 자동 생성됩니다.
                    """
    )
    @PostMapping
    public ResponseEntity<ApiResponse<SleepResponse>> createSleep(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody SleepRequest.SleepCreate request
    ) {

        SleepResponse response =
                sleepService.createSleep(memberId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @Operation(
            summary = "수면 수정",
            description = """
                    기존에 입력한 밤잠 또는 낮잠 수면 세션 정보를 수정합니다.<br>
                    사용자는 수정할 총 수면 시간, 기상 시각, 낮잠 여부를 입력합니다.<br><br>
                    
                    - bed_time은 요청값으로 받지 않습니다.<br>
                    - 서버에서 wake_up_time - duration_minutes 값으로 bed_time을 다시 계산합니다.<br>
                    - duration_minutes가 180분을 초과하면 낮잠으로 저장되지 않습니다.<br>
                    - created_at은 최초 생성 시각이므로 수정해도 변경되지 않습니다.
                    """
    )
    @PatchMapping("/{sleepId}")
    public ResponseEntity<ApiResponse<SleepResponse>> updateSleep(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long sleepId,
            @Valid @RequestBody SleepRequest.SleepUpdate request
    ) {

        SleepResponse response =
                sleepService.updateSleep(memberId, sleepId, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "수면 삭제",
            description = """
                기존에 입력한 밤잠 또는 낮잠 수면 세션을 삭제합니다.<br>
                삭제 대상은 sleepId로 구분합니다.<br><br>
                
                - 삭제 후 해당 날짜의 수면 부족 패널티 및 종합 컨디션 점수 재계산 수정 예정
                """
    )
    @DeleteMapping("/{sleepId}")
    public ResponseEntity<ApiResponse<Void>> deleteSleep(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long sleepId
    ) {

        sleepService.deleteSleep(memberId, sleepId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}