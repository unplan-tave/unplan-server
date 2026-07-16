package com.unplan.unplanserver.domain.schedule.controller;

import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleSearchCondition;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.unplan.unplanserver.domain.schedule.dto.response.*;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.service.ScheduleSearchService;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import com.unplan.unplanserver.global.response.ApiResponse;
import com.unplan.unplanserver.global.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Tag(name = "Schedule CRUD", description = "일정 CRUD API")
@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleSearchService scheduleSearchService;

    @Operation(summary = "일정 필터 검색",
            description = "저장된 일정 카드를 키워드·필터로 검색해 날짜 오름차순으로 페이지네이션(30개)해 반환합니다. "
                    + "필터는 넘어온 것만 AND 로 조합되며, status·conditionTags·personalTags 는 복수 지정 시 OR 입니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ScheduleSearchResponse>>> searchSchedules(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "제목 검색 키워드") @RequestParam(required = false) String keyword,
            @Parameter(description = "true=큐 카드, false=핀 카드") @RequestParam(required = false) Boolean isQueue,
            @Parameter(description = "진행 상태(TODO/IN_PROGRESS/DONE), 복수 가능") @RequestParam(required = false) List<ScheduleStatus> status,
            @Parameter(description = "컨디션 태그, 복수 가능") @RequestParam(required = false) List<ConditionTag> conditionTags,
            @Parameter(description = "개인 태그 이름, 복수 가능") @RequestParam(required = false) List<String> personalTags,
            @Parameter(description = "페이지 번호(0부터, 기본 0)") @RequestParam(required = false) Integer page) {

        ScheduleSearchCondition condition =
                new ScheduleSearchCondition(keyword, isQueue, status, conditionTags, personalTags);
        return ResponseEntity.ok(ApiResponse.success(
                scheduleSearchService.search(memberId, condition, page)));
    }

    @Operation(summary = "일정 생성", description = "새로운 일정을 생성합니다. 시작/종료 시간이 없으면 큐카드로 등록됩니다.")
    @PostMapping
    public ResponseEntity<ScheduleCreateResponse> createSchedule(
            @AuthenticationPrincipal Long memberId,
            @RequestBody @Valid ScheduleCreateRequest request) {

        ScheduleCreateResponse response = scheduleService.createSchedule(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "일정 일별 조회", description = "특정 날짜의 일정 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<List<ScheduleGetResponse>> getSchedulesByDate(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "조회할 날짜 (yyyy-MM-dd)", example = "2026-06-20")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(scheduleService.getSchedulesByDate(memberId, date));
    }


    @Operation(summary = "일정 주별 조회", description = "선택한 날짜가 포함된 주(일~토)의 일정 목록을 조회합니다.")
    @GetMapping("/weekly")
    public ResponseEntity<ScheduleWeeklyResponse> getSchedulesByWeek(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "조회 기준 날짜 (yyyy-MM-dd)", example = "2026-06-20")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(scheduleService.getSchedulesByWeek(memberId, date));
    }

    @Operation(summary = "일정 월별 조회", description = "해당 월의 캘린더 뷰 기준(첫째 주 일요일 ~ 마지막 주 토요일) 날짜별 일정 개수를 조회합니다.")
    @GetMapping("/monthly")
    public ResponseEntity<ScheduleMonthlyResponse> getSchedulesByMonth(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "조회할 년/월 (yyyy-MM)", example = "2026-06")
            @RequestParam String month) {

        try {
            return ResponseEntity.ok(scheduleService.getSchedulesByMonth(memberId, YearMonth.parse(month)));
        } catch (java.time.format.DateTimeParseException e) {
            throw new CustomException(ErrorCode.INVALID_MONTH_FORMAT);
        }
    }

    @Operation(summary = "개인 태그 목록 조회", description = "로그인한 멤버가 등록한 개인 태그 전체를 조회합니다. (태그 검색/재사용 화면용)")
    @GetMapping("/tags")
    public ResponseEntity<List<PersonalTagResponse>> getPersonalTags(
            @AuthenticationPrincipal Long memberId) {

        return ResponseEntity.ok(scheduleService.getPersonalTags(memberId));
    }

    @Operation(summary = "일정 상세 조회", description = "특정 일정의 상세 정보를 조회합니다.")
    @GetMapping("/{scheduleId}")
    public ResponseEntity<ScheduleDetailResponse> getScheduleDetail(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "조회할 일정 ID", example = "1")
            @PathVariable Long scheduleId) {

        return ResponseEntity.ok(scheduleService.getScheduleDetail(memberId, scheduleId));
    }

    @Operation(summary = "일정 수정", description = "특정 일정의 정보를 수정합니다. 전달한 필드만 업데이트됩니다. personalTags를 전달하면 태그 전체가 해당 목록으로 교체됩니다.")
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<ScheduleDetailResponse> updateSchedule(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "수정할 일정 ID", example = "1")
            @PathVariable Long scheduleId,
            @RequestBody @Valid ScheduleUpdateRequest request) {

        return ResponseEntity.ok(scheduleService.updateSchedule(memberId, scheduleId, request));
    }

    @Operation(summary = "일정 삭제", description = "특정 일정을 삭제합니다.")
    @DeleteMapping("/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "삭제할 일정 ID", example = "1")
            @PathVariable Long scheduleId) {

        scheduleService.deleteSchedule(memberId, scheduleId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/message")
    public ResponseEntity<ApiResponse<DailyMessageResponseDto>> getDailyMessage(@AuthenticationPrincipal Long memberId, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(scheduleService.getDailyMessage(memberId, date)));
    }
}