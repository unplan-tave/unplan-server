package com.unplan.unplanserver.domain.schedule.controller;

import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleCreateResponse;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleDetailResponse;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleGetResponse;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
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

    // 일정 생성
    @PostMapping
    public ResponseEntity<ScheduleCreateResponse> createSchedule(
            @RequestBody @Valid ScheduleCreateRequest request) {

        // 임시 memberId (추후 JWT에서 추출 예정)
        Long memberId = 1L;

        ScheduleCreateResponse response = scheduleService.createSchedule(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 일정 조회
    @GetMapping
    public ResponseEntity<List<ScheduleGetResponse>> getSchedulesByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        // 임시 memberId (추후 JWT에서 추출 예정)
        Long memberId = 1L;

        return ResponseEntity.ok(scheduleService.getSchedulesByDate(memberId, date));
    }

    // 일정 상세 조회
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleDetailResponse> getScheduleDetail(
            @PathVariable Long scheduleId) {

        // 임시 memberId (추후 JWT에서 추출 예정)
        Long memberId = 1L;

        return ResponseEntity.ok(scheduleService.getScheduleDetail(memberId, scheduleId));
    }

    // 일정 수정
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<ScheduleDetailResponse> updateSchedule(
            @PathVariable Long scheduleId,
            @RequestBody ScheduleUpdateRequest request) {

        // 임시 memberId (추후 JWT에서 추출 예정)
        Long memberId = 1L;

        return ResponseEntity.ok(scheduleService.updateSchedule(memberId, scheduleId, request));
    }

    // 일정 삭제
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long scheduleId) {

        // 임시 memberId (추후 JWT에서 추출 예정)
        Long memberId = 1L;

        scheduleService.deleteSchedule(memberId, scheduleId);
        return ResponseEntity.noContent().build();
    }
}