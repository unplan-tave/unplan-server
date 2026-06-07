package com.unplan.unplanserver.domain.memo.controller;

import com.unplan.unplanserver.domain.memo.dto.request.MemoRequest;
import com.unplan.unplanserver.domain.memo.dto.response.MemoResponse;
import com.unplan.unplanserver.domain.memo.service.MemoService;
import io.swagger.v3.oas.annotations.Operation;
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
    public ResponseEntity<MemoResponse> createMemo(
            // @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemoRequest.Create request
    ) {
        Long memberId = 1L;
        MemoResponse response = memoService.createMemo(memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "메모 조회", description = "해당 날짜에 등록된 내 메모 리스트를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<MemoResponse>> getMemos(
            // @AuthenticationPrincipal Long memberId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long memberId = 1L;
        List<MemoResponse> response = memoService.getMemos(memberId, date);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "메모 다중 삭제", description = "선택한 메모들을 한 번에 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<Void> deleteMemos(
            // @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemoRequest.Delete request
    ) {
        Long memberId = 1L;
        memoService.deleteMemos(memberId, request);

        return ResponseEntity.ok().build();
    }

}
