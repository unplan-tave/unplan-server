package com.unplan.unplanserver.domain.member;


import com.unplan.unplanserver.domain.auth.dto.response.TokenReissueResponseDto;
import com.unplan.unplanserver.domain.auth.service.AuthService;
import com.unplan.unplanserver.domain.jwt.entity.Refresh;
import com.unplan.unplanserver.domain.jwt.repository.RefreshRepository;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.enums.Role;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.domain.member.scheduler.MemberCleanupScheduler;
import com.unplan.unplanserver.domain.setting.repository.SettingRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import com.unplan.unplanserver.util.JwtUtil;
import com.unplan.unplanserver.webclient.KakaoAuthClient;
import com.unplan.unplanserver.util.GoogleIdTokenValidator;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private RefreshRepository refreshRepository;
    @Mock private KakaoAuthClient kakaoAuthClient;
    @Mock private MemberRepository memberRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private GoogleIdTokenValidator googleIdTokenValidator;
    @Mock private SettingRepository settingRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("리프레시 토큰 재발급 시 기존 토큰이 삭제된다")
    void reissueDeletesOldRefreshToken() {
        String oldRefreshToken = "old-refresh-token";
        String bearerToken = "Bearer " + oldRefreshToken;
        String deviceId = "device-1";
        Long memberId = 1L;

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(memberId.toString());
        when(jwtUtil.parseClaims(oldRefreshToken, false)).thenReturn(claims);
        when(refreshRepository.existsByMemberIdAndDeviceIdAndToken(memberId, deviceId, oldRefreshToken)).thenReturn(true);

        Member member = mock(Member.class);
        when(member.getMemberId()).thenReturn(memberId);
        when(member.getRole()).thenReturn(Role.USER);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        when(jwtUtil.createJwt(any(), any(), eq(true))).thenReturn("new-access-token");
        when(jwtUtil.createJwt(any(), any(), eq(false))).thenReturn("new-refresh-token");
        Claims newClaims = mock(Claims.class);
        when(jwtUtil.parseClaims("new-refresh-token", false)).thenReturn(newClaims);
        when(jwtUtil.getIssuedAt(newClaims)).thenReturn(LocalDateTime.now());
        when(jwtUtil.getExpiration(newClaims)).thenReturn(LocalDateTime.now().plusDays(30));

        TokenReissueResponseDto result = authService.reissue(deviceId, bearerToken);

        verify(refreshRepository).deleteByToken(oldRefreshToken);
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("유효하지 않은 리프레시 토큰으로 재발급 시 예외가 발생한다")
    void reissueThrowsWhenTokenNotInDb() {
        String refreshToken = "invalid-token";
        String bearerToken = "Bearer " + refreshToken;
        String deviceId = "device-1";
        Long memberId = 1L;

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(memberId.toString());
        when(jwtUtil.parseClaims(refreshToken, false)).thenReturn(claims);
        when(refreshRepository.existsByMemberIdAndDeviceIdAndToken(memberId, deviceId, refreshToken)).thenReturn(false);

        assertThatThrownBy(() -> authService.reissue(deviceId, bearerToken))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.INVALID_TOKEN.getMessage());
    }

    @Test
    @DisplayName("탈퇴 시 리프레시 토큰이 삭제된다")
    void withdrawDeletesRefreshTokens() {
        Long memberId = 1L;
        Member member = new Member();
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        authService.withdraw(memberId);

        verify(refreshRepository).deleteByMemberId(memberId);
        verify(memberRepository).delete(member);
    }
}