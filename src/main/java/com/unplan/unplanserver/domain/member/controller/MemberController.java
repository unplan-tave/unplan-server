package com.unplan.unplanserver.domain.member.controller;

import com.unplan.unplanserver.domain.member.dto.GetProfileResponseDto;
import com.unplan.unplanserver.domain.member.dto.UpdateProfileRequestDto;
import com.unplan.unplanserver.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/profile")
    public ResponseEntity<GetProfileResponseDto> getProfile(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getProfile(memberId));
    }

    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal Long memberId, @RequestBody @Valid UpdateProfileRequestDto requestDto) {
        String name = requestDto.name();
        String nickname = requestDto.nickname();
        String email = requestDto.email();
        memberService.updateProfile(memberId, requestDto);
        return ResponseEntity.noContent().build();
    }
}
