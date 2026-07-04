package com.unplan.unplanserver.domain.memo.controller;

import com.unplan.unplanserver.domain.memo.dto.request.MemoRequest;
import com.unplan.unplanserver.domain.memo.dto.response.MemoResponse;
import com.unplan.unplanserver.domain.memo.service.MemoService;
import com.unplan.unplanserver.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Daily Memo", description = "일별 메모 API (yyyy-MM-dd 형식으로 넣어 주세요.)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/daily-memo")
public class MemoController {

    private final MemoService memoService;

    @Operation(summary = "메모 생성", description = "특정 날짜에 새로운 메모를 생성합니다. (최대 5개 제한)")
    @PostMapping
    public ResponseEntity<ApiResponse<MemoResponse>> createMemo(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemoRequest.Create request
    ) {
        MemoResponse response = memoService.createMemo(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "메모 조회", description = "해당 날짜에 등록된 내 메모 리스트를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MemoResponse>>> getMemos(
            @AuthenticationPrincipal Long memberId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<MemoResponse> response = memoService.getMemos(memberId, date);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "메모 다중 삭제", description = "선택한 메모들을 한 번에 삭제합니다.<br>" +
            " 스웨거 테스트 할 땐 ids 한 칸에 id 하나만 입력해 주세요. 여러 개 입력하실 경우 Add integer item으로 추가하시면 됩니다.")

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteMemos(
            @AuthenticationPrincipal Long memberId,
            @Parameter(description = "삭제할 메모 ID 리스트 (콤마로 구분, 예: 1,2,3)", example = "1,2,3")
            @RequestParam("ids") List<Long> ids
    ) {

        MemoRequest.Delete request = MemoRequest.Delete.builder()
                .dailyMemoIds(ids)
                .build();

        memoService.deleteMemos(memberId, request);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

}