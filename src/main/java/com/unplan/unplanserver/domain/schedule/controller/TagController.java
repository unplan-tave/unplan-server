package com.unplan.unplanserver.domain.schedule.controller;

import com.unplan.unplanserver.domain.schedule.dto.request.TagRecommendationRequestDto;
import com.unplan.unplanserver.domain.schedule.dto.response.TagRecommendationResponseDto;
import com.unplan.unplanserver.domain.schedule.service.TagService;
import com.unplan.unplanserver.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class TagController {
    private final TagService tagService;

    @PostMapping("/tag-recommendation")
    public ResponseEntity<ApiResponse<TagRecommendationResponseDto>> recommendTag(@RequestBody @Valid TagRecommendationRequestDto requestDto) {
        TagRecommendationResponseDto responseDto = tagService.recommendTag(requestDto);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }
}
