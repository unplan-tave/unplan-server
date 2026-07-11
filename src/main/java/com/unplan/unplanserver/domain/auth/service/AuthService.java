package com.unplan.unplanserver.domain.auth.service;

import com.unplan.unplanserver.domain.auth.dto.*;
import com.unplan.unplanserver.domain.auth.dto.request.GoogleLoginRequestDto;
import com.unplan.unplanserver.domain.auth.dto.request.KakaoLoginRequestDto;
import com.unplan.unplanserver.domain.auth.dto.response.KakaoUserInfoResponseDto;
import com.unplan.unplanserver.domain.auth.dto.response.SocialLoginResponseDto;
import com.unplan.unplanserver.domain.auth.dto.response.TokenReissueResponseDto;
import com.unplan.unplanserver.domain.auth.webclient.KakaoAuthClient;
import com.unplan.unplanserver.domain.jwt.entity.Refresh;
import com.unplan.unplanserver.domain.jwt.repository.RefreshRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.domain.setting.entity.Setting;
import com.unplan.unplanserver.domain.setting.repository.SettingRepository;
import com.unplan.unplanserver.global.common.TokenPair;
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
    private final SettingRepository settingRepository;

    @Transactional
    public SocialLoginResponseDto kakaoLogin(KakaoLoginRequestDto requestDto) {
        KakaoUserInfoResponseDto kakaoUserInfo = kakaoAuthClient.getUserInfo(requestDto.kakaoAccessToken());
        String oauthId = kakaoUserInfo.getId().toString();
        Member member = memberRepository.findByOauthId(oauthId).orElse(null);
        Boolean isNewMember = false;
        // 이전에 로그인한적이 없으면
        if (member == null){
            isNewMember = true;
            // 회원가입(DB에 추가)
            member = Member.fromKakao(kakaoUserInfo);
            memberRepository.save(member);
            Setting setting = new Setting(member.getMemberId());
            settingRepository.save(setting);
        }
        //로그인
        else{
            //기존 refresh토큰 삭제, memberId와 deviceId로 찾으므로 중복로그인 허용
            refreshRepository.deleteByMemberIdAndDeviceId(member.getMemberId(), requestDto.deviceId());
        }
        Boolean onboardingCompleted = member.getOnboardingCompleted();
        TokenPair tokenPair = issueTokens(member, requestDto.deviceId());
        return new SocialLoginResponseDto(tokenPair.accessToken(), tokenPair.refreshToken(), isNewMember, onboardingCompleted);

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
            Setting setting = new Setting(member.getMemberId());
            settingRepository.save(setting);
            isNewMember = true;
        }
        // 로그인
        else{
            refreshRepository.deleteByMemberIdAndDeviceId(member.getMemberId(), requestDto.deviceId());
        }
        Boolean onboardingCompleted = member.getOnboardingCompleted();
        // access, refresh 토큰 발급
        TokenPair tokenPair = issueTokens(member, requestDto.deviceId());
        return new SocialLoginResponseDto(tokenPair.accessToken(), tokenPair.refreshToken(), isNewMember, onboardingCompleted);
    }
    private TokenPair issueTokens(Member member, String deviceId){
        Long memberId = member.getMemberId();
        String role = member.getRole().toString();
        String accessToken = jwtUtil.createJwt(memberId, role, true);
        String refreshToken = jwtUtil.createJwt(memberId, role, false);

        //refresh토큰 db에 저장
        Claims claims = jwtUtil.parseClaims(refreshToken, false);
        LocalDateTime createdAt = jwtUtil.getIssuedAt(claims);
        LocalDateTime expiresAt = jwtUtil.getExpiration(claims);
        Refresh refresh = Refresh.of(memberId, refreshToken, deviceId, createdAt, expiresAt);
        refreshRepository.save(refresh);

        return new TokenPair(accessToken, refreshToken);
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

    @Transactional
    public TokenReissueResponseDto reissue(String deviceId, String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ") || bearerToken.length() <= 7) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        String refreshToken = bearerToken.substring(7);
        Claims claims = jwtUtil.parseClaims(refreshToken, false);
        if (claims == null) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        Long memberId = Long.parseLong(claims.getSubject());
        boolean isValid = refreshRepository.existsByMemberIdAndDeviceIdAndToken(memberId, deviceId, refreshToken);
        if(!isValid){
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
        refreshRepository.deleteByToken(refreshToken);  //refresh 토큰 삭제
        Member member = memberRepository.findById(memberId).orElseThrow(()-> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        TokenPair tokenPair = issueTokens(member, deviceId);
        return new TokenReissueResponseDto(tokenPair.accessToken(), tokenPair.refreshToken());
    }
}
