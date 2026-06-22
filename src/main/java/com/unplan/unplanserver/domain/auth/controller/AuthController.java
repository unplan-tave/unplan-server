package com.unplan.unplanserver.domain.auth.controller;

import com.unplan.unplanserver.domain.auth.dto.GoogleLoginRequestDto;
import com.unplan.unplanserver.domain.auth.dto.KakaoLoginRequestDto;
import com.unplan.unplanserver.domain.auth.dto.SocialLoginResponseDto;
import com.unplan.unplanserver.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;

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
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal @Valid Long memberId){
        authService.withdraw(memberId);
        return ResponseEntity.noContent().build();
    }
}
