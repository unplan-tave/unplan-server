package com.unplan.unplanserver.domain.auth.service;

import com.unplan.unplanserver.domain.auth.dto.*;
import com.unplan.unplanserver.domain.auth.webclient.KakaoAuthClient;
import com.unplan.unplanserver.domain.jwt.entity.Refresh;
import com.unplan.unplanserver.domain.jwt.repository.RefreshRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import com.unplan.unplanserver.util.GoogleIdTokenValidator;
import com.unplan.unplanserver.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class AuthService {
    private final RefreshRepository refreshRepository;
    private final KakaoAuthClient kakaoAuthClient;
    private final MemberRepository memberRepository;
    private final JwtUtil jwtUtil;
    private final GoogleIdTokenValidator googleIdTokenValidator;

    @Transactional
    public SocialLoginResponseDto kakaoLogin(KakaoLoginRequestDto requestDto) {
        KakaoUserInfoResponseDto kakaoUserInfo = kakaoAuthClient.getUserInfo(requestDto.kakaoAccessToken());
        String oauthId = kakaoUserInfo.getId().toString();
        Member member = memberRepository.findByOauthId(oauthId).orElse(null);
        Boolean isNewMember = false;
        // 회원가입
        if (member == null){
            isNewMember = true;
            // 회원가입(DB에 추가)
            member = Member.fromKakao(kakaoUserInfo);
            memberRepository.save(member);
        }
        //로그인
        else{
            //기존 refresh토큰 삭제, memberId와 deviceId로 찾으므로 중복로그인 허용
            refreshRepository.deleteByMemberIdAndDeviceId(member.getMemberId(), requestDto.deviceId());
        }

        return issueTokens(member, requestDto.deviceId(), isNewMember);

    }

    @Transactional
    public SocialLoginResponseDto googleLogin(GoogleLoginRequestDto requestDto) {
        String googleIdToken = requestDto.googleIdToken();
        GoogleUserInfoDto googleUserInfoDto = googleIdTokenValidator.isValid(googleIdToken);
        Member member = memberRepository.findByOauthId(googleUserInfoDto.getOauthId()).orElse(null);
        Boolean isNewMember = false;
        //회원가입
        if(member == null){
            member = Member.fromGoogle(googleUserInfoDto);
            memberRepository.save(member);
            isNewMember = true;
        }
        // 로그인
        else{
            refreshRepository.deleteByMemberIdAndDeviceId(member.getMemberId(), requestDto.deviceId());
        }
        // access, refresh 토큰 발급
        return issueTokens(member, requestDto.deviceId(), isNewMember);
    }
    private SocialLoginResponseDto issueTokens(Member member, String deviceId, Boolean isNewUser){
        Long memberId = member.getMemberId();
        String role = member.getRole().toString();
        String accessToken = jwtUtil.createJwt(memberId, role, true);
        String refreshToken = jwtUtil.createJwt(memberId, role, false);

        Claims claims = jwtUtil.parseClaims(refreshToken, false);
        LocalDateTime createdAt = jwtUtil.getIssuedAt(claims);
        LocalDateTime expiresAt = jwtUtil.getExpiration(claims);
        Refresh refresh = Refresh.of(memberId, refreshToken, deviceId, createdAt, expiresAt);
        refreshRepository.save(refresh);

        return new SocialLoginResponseDto(accessToken, refreshToken, isNewUser);
    }

    @Transactional
    public void logout(Long memberId, String deviceId) {
        refreshRepository.deleteByMemberIdAndDeviceId(memberId, deviceId);
    }

    @Transactional
    public void withdraw(Long memberId) {
        refreshRepository.deleteByMemberId(memberId);
        Member member = memberRepository.findById(memberId)
                .orElseThrow(()->new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        memberRepository.delete(member);
    }
}
