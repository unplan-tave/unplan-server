package com.unplan.unplanserver.domain.member.service;

import com.unplan.unplanserver.domain.member.controller.MemberController;
import com.unplan.unplanserver.domain.member.dto.ProfileResponseDto;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MemberService {
    private MemberRepository memberRepository;
    @Transactional
    public ProfileResponseDto getProfile(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                ()->new CustomException(ErrorCode.MEMBER_NOT_FOUND)
        );
        String name = member.getName();
        String nickname = member.getNickname();
        String email = member.getEmail();
        return new ProfileResponseDto(name, nickname, email);
    }
}
