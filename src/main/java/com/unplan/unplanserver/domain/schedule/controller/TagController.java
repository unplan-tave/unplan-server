package com.unplan.unplanserver.domain.schedule.controller;

import com.unplan.unplanserver.domain.schedule.dto.request.TagRecommendationRequestDto;
import com.unplan.unplanserver.domain.schedule.dto.response.TagRecommendationResponseDto;
import com.unplan.unplanserver.domain.schedule.service.TagService;
import com.unplan.unplanserver.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tag", description = "일정 제목 기반 컨디션 태그 추천 API")
@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @Operation(summary = "일정 제목 기반 태그 추천", description = "일정 카드 편집화면에서 사용자가 입력한 일정 제목을 기반으로 컨디션 태그를 추천합니다.")
    @PostMapping("/tag-recommendation")
    public ResponseEntity<ApiResponse<TagRecommendationResponseDto>> recommendTag(@RequestBody @Valid TagRecommendationRequestDto requestDto) {
        TagRecommendationResponseDto responseDto = tagService.recommendTag(requestDto);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }
}
