package com.unplan.unplanserver.domain.auth.controller;

import com.unplan.unplanserver.domain.auth.dto.SocialLoginResponseDto;
import com.unplan.unplanserver.domain.auth.service.AuthService;
import com.unplan.unplanserver.util.JwtUtil;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@Profile({"local", "dev"})
@RestController
public class TestController {
    private final JwtUtil jwtUtil;

    @PostMapping("/auth/accessToken")
    public ResponseEntity<SocialLoginResponseDto> getAccessToken(@RequestParam Long memberId){
        return ResponseEntity.ok(new SocialLoginResponseDto(
                jwtUtil.createJwt(memberId, "USER", true),
                "refreshToken이 아닙니다",
                null));
    }
}
