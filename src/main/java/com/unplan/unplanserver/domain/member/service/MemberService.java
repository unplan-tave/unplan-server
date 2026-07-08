package com.unplan.unplanserver.domain.member.service;

import com.unplan.unplanserver.domain.member.dto.GetProfileResponseDto;
import com.unplan.unplanserver.domain.member.dto.UpdateProfileRequestDto;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    @Transactional(readOnly = true)
    public GetProfileResponseDto getProfile(Long memberId) {
        if (memberId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberRepository.findById(memberId).orElseThrow(
                ()->new CustomException(ErrorCode.MEMBER_NOT_FOUND)
        );
        String name = member.getName();
        String nickname = member.getNickname();
        String email = member.getEmail();
        Boolean onboardingCompleted = member.getOnboardingCompleted();
        return new GetProfileResponseDto(name, nickname, email, onboardingCompleted);
    }

    @Transactional
    public void updateProfile(Long memberId, UpdateProfileRequestDto requestDto) {
        if (memberId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberRepository.findById(memberId).orElseThrow(()->new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        if (requestDto.email() != null && !requestDto.email().trim().isEmpty() && !requestDto.email().equals(member.getEmail())) {
            Member duplicateEmailMember = memberRepository.findByEmail(requestDto.email()).orElse(null);
            if (duplicateEmailMember != null && !duplicateEmailMember.getMemberId().equals(member.getMemberId())) {
                throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
            }
        }
        member.updateProfile(requestDto);
    }

    public void completeOnboarding(Long memberId) {
        if (memberId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        member.completeOnboarding();
    }
}
