package com.unplan.unplanserver.domain.measurement.controller;

import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse;
import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse.MeasurementAverageResponse;
import com.unplan.unplanserver.domain.measurement.service.MeasurementService;
import com.unplan.unplanserver.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Measurement", description = "컨디션/수면 통합 기록 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/measurements")
public class MeasurementController {

    private final MeasurementService measurementService;

    @Operation(
            summary = "하루 기록 목록 조회",
            description = """
                    특정 날짜의 컨디션 기록과 수면 기록을 함께 조회합니다.<br>
                    컨디션 기록은 dateTime의 날짜 기준, 수면 기록은 wakeUpTime의 날짜 기준으로 포함합니다.<br>
                    YYYY-MM-DD 형식으로 입력하면 됩니다.
                    """
    )
    @GetMapping
    public ResponseEntity<ApiResponse<MeasurementRecordResponse>> getDailyRecord(
            @AuthenticationPrincipal Long memberId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {

        MeasurementRecordResponse response =
                measurementService.getDailyRecord(memberId, date);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "평균 보기 조회",
            description = """
                    지정한 기간의 평균 컨디션 점수, Body/Mind 퍼센트, 수면 점수, 수면 시간 평균을 조회합니다.<br>
                    groupBy 값에 따라 DAY, WEEK, MONTH 단위로 집계합니다.<br>
                    from과 to는 YYYY-MM-DD 형식으로 입력하면 됩니다.
                    """
    )
    @GetMapping("/averages")
    public ResponseEntity<ApiResponse<MeasurementAverageResponse>> getAverageRecords(
            @AuthenticationPrincipal Long memberId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(
                    description = "조회할 평균 타입",
                    schema = @Schema(allowableValues = {"ALL", "CONDITION", "SLEEP"})
            )
            @RequestParam("type") String type,
            @Parameter(
                    description = "평균 집계 단위",
                    schema = @Schema(allowableValues = {"DAY", "WEEK", "MONTH"})
            )
            @RequestParam("groupBy") String groupBy
    ) {

        MeasurementAverageResponse response =
                measurementService.getAverageRecords(memberId, from, to, type, groupBy);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
