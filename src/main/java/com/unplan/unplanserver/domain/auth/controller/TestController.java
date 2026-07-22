package com.unplan.unplanserver.domain.auth.controller;

import com.unplan.unplanserver.domain.auth.dto.response.SocialLoginResponseDto;
import com.unplan.unplanserver.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Test", description = "테스트용 API (local, dev 환경 전용)")
@AllArgsConstructor
@Profile({"local", "dev"})
@RestController
public class TestController {
    private final JwtUtil jwtUtil;

    @Operation(summary = "테스트용 액세스 토큰 발급", description = "memberId로 JWT 액세스 토큰을 바로 발급합니다. local/dev 환경에서만 동작합니다.")
    @PostMapping("/auth/accessToken")
    public ResponseEntity<SocialLoginResponseDto> getAccessToken(@RequestParam Long memberId){
        return ResponseEntity.ok(new SocialLoginResponseDto(
                jwtUtil.createJwt(memberId, "USER", true),
                "refreshToken이 아닙니다",
                false, false));
    }
}
