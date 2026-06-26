package com.unplan.unplanserver.domain.member.service;

import com.unplan.unplanserver.domain.member.dto.ProfileResponseDto;
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
    public ProfileResponseDto getProfile(Long memberId) {
        if (memberId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        Member member = memberRepository.findById(memberId).orElseThrow(
                ()->new CustomException(ErrorCode.MEMBER_NOT_FOUND)
        );
        String name = member.getName();
        String nickname = member.getNickname();
        String email = member.getEmail();
        return new ProfileResponseDto(name, nickname, email);
    }
}
