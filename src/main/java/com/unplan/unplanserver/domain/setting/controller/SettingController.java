package com.unplan.unplanserver.domain.setting.controller;

import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingRequestDto;
import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingResponseDto;
import com.unplan.unplanserver.domain.setting.service.SettingService;
import com.unplan.unplanserver.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member-settings")
@RequiredArgsConstructor
public class SettingController {
    private final SettingService settingService;
    @PatchMapping("/empty-time")
    public ResponseEntity<ApiResponse<EmptyTimeSettingResponseDto>> updateEmptyTimeRecommendSetting(@AuthenticationPrincipal Long memberId, @RequestBody EmptyTimeSettingRequestDto requestDto) {
        EmptyTimeSettingResponseDto responseDto = settingService.updateSetting(memberId, requestDto);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(responseDto));
    }

    @GetMapping("/empty-time")
    public ResponseEntity<ApiResponse<EmptyTimeSettingResponseDto>> getEmptyTimeRecommendSetting(@AuthenticationPrincipal Long memberId) {
        EmptyTimeSettingResponseDto responseDto = settingService.getSetting(memberId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(responseDto));
    }

}
