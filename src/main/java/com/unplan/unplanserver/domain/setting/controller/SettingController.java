package com.unplan.unplanserver.domain.setting.controller;

import com.unplan.unplanserver.domain.setting.dto.AlarmSettingRequestDto;
import com.unplan.unplanserver.domain.setting.dto.AlarmSettingResponseDto;
import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingRequestDto;
import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingResponseDto;
import com.unplan.unplanserver.domain.setting.service.SettingService;
import com.unplan.unplanserver.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Setting", description = "알림 설정 및 비어있는 시간 설정 API")
@RestController
@RequestMapping("/member-settings")
@RequiredArgsConstructor
public class SettingController {
    private final SettingService settingService;
    @Operation(summary = "빈 시간 추천 설정 변경", description = "설정 > 추천 개인화 설정 > 일정 추천 기준 화면에서 추천 조건 관련 설정들을 변경합니다")
    @PatchMapping("/empty-time")
    public ResponseEntity<ApiResponse<EmptyTimeSettingResponseDto>> updateEmptyTimeRecommendSetting(@AuthenticationPrincipal Long memberId, @RequestBody EmptyTimeSettingRequestDto requestDto) {
        EmptyTimeSettingResponseDto responseDto = settingService.updateSetting(memberId, requestDto);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(responseDto));
    }

    @Operation(summary = "빈 시간 추천 설정 조회", description = "설정 > 추천 개인화 설정 > 일정 추천 기준 화면에서 추천 조건 관련 설정들을 조회합니다")
    @GetMapping("/empty-time")
    public ResponseEntity<ApiResponse<EmptyTimeSettingResponseDto>> getEmptyTimeRecommendSetting(@AuthenticationPrincipal Long memberId) {
        EmptyTimeSettingResponseDto responseDto = settingService.getSetting(memberId);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(responseDto));
    }

    @Operation(summary = "알림 설정 변경", description = "설정 탭의 알림 설정을 변경합니다.")
    @PatchMapping("/alarm-setting")
    public ResponseEntity<ApiResponse<AlarmSettingResponseDto>> updateAlarmSetting(@AuthenticationPrincipal Long memberId, @RequestBody AlarmSettingRequestDto requestDto) {
        AlarmSettingResponseDto responseDto = settingService.updateAlarmSetting(memberId, requestDto);
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(responseDto));
    }

    @Operation(summary = "알림 설정 조회", description = "설정 화면의 사용자가 설정한 알림 설정 목록을 조회합니다.")
    @GetMapping("/alarm-setting")
    public ResponseEntity<ApiResponse<AlarmSettingResponseDto>> getAlarmSetting(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(settingService.getAlarmSetting(memberId)));
    }

}
