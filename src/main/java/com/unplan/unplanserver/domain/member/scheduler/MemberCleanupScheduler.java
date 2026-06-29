package com.unplan.unplanserver.domain.member.scheduler;

import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemberCleanupScheduler {
    MemberRepository memberRepository;
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteWithdrawnMembers() {
        int gracePeriodDays = 30;
        LocalDateTime threshold = LocalDateTime.now().minusDays(gracePeriodDays);
        List<Member> members = memberRepository.findByDeletedAtBefore(threshold);

        if (!members.isEmpty()) {
            for (Member member : members) {
                log.info("탈퇴 회원 삭제 - memberId: {}, name: {}, nickname: {}, email: {}, oauthId: {}, provider: {}, role: {}, targetSleepTime: {}, createdAt: {}",
                        member.getMemberId(),
                        member.getName(),
                        member.getNickname(),
                        member.getEmail(),
                        member.getOauthId(),
                        member.getProvider(),
                        member.getRole(),
                        member.getCreatedAt()
                );
            }
            memberRepository.deleteWithdrawnMembers(threshold);
        }
    }
}
