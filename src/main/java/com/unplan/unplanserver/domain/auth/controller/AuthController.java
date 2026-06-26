package com.unplan.unplanserver.domain.auth.controller;

import com.unplan.unplanserver.domain.auth.dto.request.GoogleLoginRequestDto;
import com.unplan.unplanserver.domain.auth.dto.request.KakaoLoginRequestDto;
import com.unplan.unplanserver.domain.auth.dto.request.TokenReissueRequestDto;
import com.unplan.unplanserver.domain.auth.dto.response.SocialLoginResponseDto;
import com.unplan.unplanserver.domain.auth.dto.response.TokenReissueResponseDto;
import com.unplan.unplanserver.domain.auth.service.AuthService;
import com.unplan.unplanserver.domain.member.dto.LogoutRequestDto;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private AuthService authService;
    @PostMapping("/kakao")
    public ResponseEntity<SocialLoginResponseDto> kakaoLogin(@RequestBody @Valid KakaoLoginRequestDto requestDto){
        return ResponseEntity.ok(authService.kakaoLogin(requestDto));
    }

    @PostMapping("/google")
    public ResponseEntity<SocialLoginResponseDto> googleLogin(@RequestBody @Valid GoogleLoginRequestDto requestDto) {
        return ResponseEntity.ok(authService.googleLogin(requestDto));
    }

    @PatchMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long memberId){
        authService.withdraw(memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Long memberId, @RequestBody @Valid LogoutRequestDto requestDto){
        authService.logout(memberId, requestDto.deviceId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reissue")
    public ResponseEntity<TokenReissueResponseDto> reissue(@RequestHeader("Authorization") String bearerToken, @RequestBody @Valid TokenReissueRequestDto requestDto){
        return ResponseEntity.ok(authService.reissue(requestDto.deviceId(), bearerToken));
    }
}
