package com.unplan.unplanserver.domain.member.controller;

import com.unplan.unplanserver.domain.member.dto.ProfileResponseDto;
import com.unplan.unplanserver.domain.member.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/member")
public class MemberController {
    private MemberService memberService;

    @GetMapping("/profile")
    public ResponseEntity<ProfileResponseDto> getProfile(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getProfile(memberId));
    }
}
