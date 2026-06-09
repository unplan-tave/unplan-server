package com.unplan.unplanserver.domain.auth.service;

import com.unplan.unplanserver.domain.auth.dto.KakaoLoginRequestDto;
import com.unplan.unplanserver.domain.auth.dto.KakaoUserInfoResponseDto;
import com.unplan.unplanserver.domain.auth.dto.SocialLoginResponseDto;
import com.unplan.unplanserver.domain.auth.webclient.KakaoAuthClient;
import com.unplan.unplanserver.domain.jwt.entity.Refresh;
import com.unplan.unplanserver.domain.jwt.repository.RefreshRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class AuthService {
    private final RefreshRepository refreshRepository;
    private final KakaoAuthClient kakaoAuthClient;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;

    @Transactional
    public SocialLoginResponseDto kakaoLogin(KakaoLoginRequestDto requestDto) {
        KakaoUserInfoResponseDto kakaoUserInfo = kakaoAuthClient.getUserInfo(requestDto.kakaoAccessToken());
        Long oauthId = kakaoUserInfo.getId();
        Member member;
        Boolean isNewMember = false;
        // 이전에 로그인한적이 없으면
        if(!memberRepository.existsByOauthId(oauthId)){
            isNewMember = true;
            // 회원가입(DB에 추가)
            member = Member.fromKakao(kakaoUserInfo);
            memberRepository.save(member);
        }
        else{
            member = memberRepository.findByOauthId(oauthId).orElseThrow(()-> new RuntimeException("회원을 찾을 수 없습니다"));
            //기존 refresh토큰 삭제, memberId와 deviceId로 찾으므로 중복로그인 허용
            refreshRepository.deleteByMemberIdAndDeviceId(member.getMemberId(), requestDto.deviceId());
        }

        Long memberId = member.getMemberId();
        String role = member.getRole().toString();
        String accessToken = jwtUtil.createJwt(memberId, role, true);
        String refreshToken = jwtUtil.createJwt(memberId, role, false);

        Claims claims = jwtUtil.parseClaims(refreshToken, false);
        LocalDateTime createdAt = jwtUtil.getIssuedAt(claims);
        LocalDateTime expiresAt = jwtUtil.getExpiration(claims);
        Refresh refresh = Refresh.of(memberId, refreshToken, requestDto.deviceId(), createdAt, expiresAt);
        refreshRepository.save(refresh);

        return new SocialLoginResponseDto(accessToken, refreshToken, isNewMember);

    }
}
