package com.unplan.unplanserver.domain.member;

import com.unplan.unplanserver.domain.member.dto.GetProfileResponseDto;
import com.unplan.unplanserver.domain.member.dto.UpdateProfileRequestDto;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.domain.member.service.MemberService;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    @DisplayName("프로필 조회 시 회원 정보를 반환한다")
    void getProfileReturnsMemberInfo() {
        Long memberId = 1L;
        Member member = mock(Member.class);
        when(member.getName()).thenReturn("홍길동");
        when(member.getNickname()).thenReturn("길동이");
        when(member.getEmail()).thenReturn("test@test.com");
        when(member.getOnboardingCompleted()).thenReturn(true);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        GetProfileResponseDto result = memberService.getProfile(memberId);

        assertThat(result.name()).isEqualTo("홍길동");
        assertThat(result.nickname()).isEqualTo("길동이");
        assertThat(result.email()).isEqualTo("test@test.com");
        assertThat(result.onboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("memberId가 null이면 UNAUTHORIZED 예외가 발생한다")
    void getProfileThrowsWhenMemberIdIsNull() {
        assertThatThrownBy(() -> memberService.getProfile(null))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.UNAUTHORIZED.getMessage());
    }

    @Test
    @DisplayName("존재하지 않는 회원 조회 시 MEMBER_NOT_FOUND 예외가 발생한다")
    void getProfileThrowsWhenMemberNotFound() {
        when(memberRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getProfile(999L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("이메일 중복 시 DUPLICATE_EMAIL 예외가 발생한다")
    void updateProfileThrowsWhenEmailDuplicated() {
        Long memberId = 1L;
        Member member = mock(Member.class);
        when(member.getMemberId()).thenReturn(memberId);
        when(member.getEmail()).thenReturn("old@test.com");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        Member anotherMember = mock(Member.class);
        when(anotherMember.getMemberId()).thenReturn(2L);
        when(memberRepository.findByEmail("new@test.com")).thenReturn(Optional.of(anotherMember));

        UpdateProfileRequestDto requestDto = new UpdateProfileRequestDto(null, null, "new@test.com");

        assertThatThrownBy(() -> memberService.updateProfile(memberId, requestDto))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_EMAIL.getMessage());
    }

    @Test
    @DisplayName("자신의 이메일로 업데이트 시 예외가 발생하지 않는다")
    void updateProfileDoesNotThrowWhenEmailIsSelf() {
        Long memberId = 1L;
        Member member = mock(Member.class);
        when(member.getMemberId()).thenReturn(memberId);
        when(member.getEmail()).thenReturn("old@test.com");
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(memberRepository.findByEmail("new@test.com")).thenReturn(Optional.of(member));

        UpdateProfileRequestDto requestDto = new UpdateProfileRequestDto(null, null, "new@test.com");

        memberService.updateProfile(memberId, requestDto);

        verify(member).updateProfile(requestDto);
    }

    @Test
    @DisplayName("온보딩 완료 시 completeOnboarding이 호출된다")
    void completeOnboardingCallsMemberMethod() {
        Long memberId = 1L;
        Member member = mock(Member.class);
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));

        memberService.completeOnboarding(memberId);

        verify(member).completeOnboarding();
    }
}