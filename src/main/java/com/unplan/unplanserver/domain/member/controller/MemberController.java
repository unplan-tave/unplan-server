package com.unplan.unplanserver.domain.member.controller;

import com.unplan.unplanserver.domain.member.dto.GetProfileResponseDto;
import com.unplan.unplanserver.domain.member.dto.UpdateProfileRequestDto;
import com.unplan.unplanserver.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Member", description = "회원 정보 조회 및 수정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {
    private final MemberService memberService;

    @Operation(summary = "회원 정보 조회", description = "로그인한 사용자의 정보와 온보딩 완료 여부를 조회합니다")
    @GetMapping("/profile")
    public ResponseEntity<GetProfileResponseDto> getProfile(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getProfile(memberId));
    }

    @Operation(summary = "회원 정보 변경", description = "로그인한 사용자의 프로필 정보를 변경합니다")
    @PatchMapping("/profile")
    public ResponseEntity<Void> updateProfile(@AuthenticationPrincipal Long memberId, @RequestBody @Valid UpdateProfileRequestDto requestDto) {
        memberService.updateProfile(memberId, requestDto);
        return ResponseEntity.noContent().build();
    }
}
