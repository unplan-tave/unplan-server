package com.unplan.unplanserver.domain.auth.controller;

import com.unplan.unplanserver.domain.auth.dto.request.GoogleLoginRequestDto;
import com.unplan.unplanserver.domain.auth.dto.request.KakaoLoginRequestDto;
import com.unplan.unplanserver.domain.auth.dto.request.TokenReissueRequestDto;
import com.unplan.unplanserver.domain.auth.dto.response.SocialLoginResponseDto;
import com.unplan.unplanserver.domain.auth.dto.response.TokenReissueResponseDto;
import com.unplan.unplanserver.domain.auth.service.AuthService;
import com.unplan.unplanserver.domain.member.dto.LogoutRequestDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "소셜 로그인, 로그아웃, 토큰 재발급 API")
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private AuthService authService;

    @Operation(summary = "카카오 로그인", description = "카카오 access 토큰으로 로그인 및 회원가입 처리후 언플랜 JWT를 발급합니다. 최초 로그인시 회원가입 처리를 하고 회원가입 이후에는 로그인 처리됩니다.")
    @PostMapping("/kakao")
    public ResponseEntity<SocialLoginResponseDto> kakaoLogin(@RequestBody @Valid KakaoLoginRequestDto requestDto){
        return ResponseEntity.ok(authService.kakaoLogin(requestDto));
    }

    @Operation(summary = "구글 로그인", description = "구글에서 발급된 JWT로 로그인 및 회원가입 처리후 언플랜 JWT를 발급합니다. 최초 로그인시 회원가입 처리를 하고 회원가입 이후에는 로그인 처리됩니다.")
    @PostMapping("/google")
    public ResponseEntity<SocialLoginResponseDto> googleLogin(@RequestBody @Valid GoogleLoginRequestDto requestDto) {
        return ResponseEntity.ok(authService.googleLogin(requestDto));
    }

    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자를 탈퇴처리합니다. 30일 이후에는 회원 관련 데이터가 완전히 삭제됩니다")
    @PatchMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long memberId){
        authService.withdraw(memberId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 로그아웃", description = "로그인한 사용자를 로그아웃처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long memberId, @RequestBody @Valid LogoutRequestDto requestDto){
        authService.logout(memberId, requestDto.deviceId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "access 토큰 재발급", description = "access 토큰 만료시 refresh 토큰으로 access 토큰을 재발급합니다. 이때 사용된 refresh토큰은 삭제하고 access 토큰과 함께 refresh 토큰도 재발급됩니다")
    @PostMapping("/reissue")
    public ResponseEntity<TokenReissueResponseDto> reissue(@RequestHeader("Authorization") String bearerToken, @RequestBody @Valid TokenReissueRequestDto requestDto){
        return ResponseEntity.ok(authService.reissue(requestDto.deviceId(), bearerToken));
    }
}
