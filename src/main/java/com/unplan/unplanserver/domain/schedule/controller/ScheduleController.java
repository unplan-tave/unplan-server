package com.unplan.unplanserver.domain.schedule.controller;

import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleCreateResponse;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleDetailResponse;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleGetResponse;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Schedule CRUD", description = "일정 CRUD API")
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @Operation(summary = "일정 생성", description = "새로운 일정을 생성합니다. 시작/종료 시간이 없으면 큐카드로 등록됩니다.")
    @PostMapping
    public ResponseEntity<ScheduleCreateResponse> createSchedule(
            @RequestBody @Valid ScheduleCreateRequest request) {

        // 임시 memberId (추후 JWT에서 추출 예정)
        Long memberId = 1L;

        ScheduleCreateResponse response = scheduleService.createSchedule(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "일정 일별 조회", description = "특정 날짜의 일정 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ScheduleGetResponse>> getSchedulesByDate(
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2026-06-20")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // 임시 memberId (추후 JWT에서 추출 예정)
        Long memberId = 1L;

        return ResponseEntity.ok(scheduleService.getSchedulesByDate(memberId, date));
    }


@Operation(summary = "일정 상세 조회", description = "특정 일정의 상세 정보를 조회합니다.")
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleDetailResponse> getScheduleDetail(
            @Parameter(description = "조회할 일정 ID", example = "1")
            @PathVariable Long scheduleId) {

        // 임시 memberId (추후 JWT에서 추출 예정)
        Long memberId = 1L;

        return ResponseEntity.ok(scheduleService.getScheduleDetail(memberId, scheduleId));
    }

    @Operation(summary = "일정 수정", description = "특정 일정의 정보를 수정합니다. 전달한 필드만 업데이트됩니다.")
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<ScheduleDetailResponse> updateSchedule(
            @Parameter(description = "수정할 일정 ID", example = "1")
            @PathVariable Long scheduleId,
            @RequestBody ScheduleUpdateRequest request) {

        // 임시 memberId (추후 JWT에서 추출 예정)
        Long memberId = 1L;

        return ResponseEntity.ok(scheduleService.updateSchedule(memberId, scheduleId, request));
    }

    @Operation(summary = "일정 삭제", description = "특정 일정을 삭제합니다.")
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @Parameter(description = "삭제할 일정 ID", example = "1")
            @PathVariable Long scheduleId) {

        // 임시 memberId (추후 JWT에서 추출 예정)
        Long memberId = 1L;

        scheduleService.deleteSchedule(memberId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}