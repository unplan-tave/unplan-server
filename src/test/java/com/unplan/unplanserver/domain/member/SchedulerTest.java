package com.unplan.unplanserver.domain.member;

import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.domain.member.repository.MemberRepository;
import com.unplan.unplanserver.domain.member.scheduler.MemberCleanupScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulerTest {

    @Mock private MemberRepository memberRepository;

    @InjectMocks
    private MemberCleanupScheduler scheduler;

    @Test
    @DisplayName("탈퇴 후 30일이 지난 회원을 삭제한다")
    void deleteWithdrawnMembersOlderThan30Days() {
        Member member = mock(Member.class);
        when(memberRepository.findByDeletedAtBefore(any())).thenReturn(List.of(member));

        scheduler.deleteWithdrawnMembers();

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(memberRepository).deleteWithdrawnMembers(captor.capture());
        assertThat(captor.getValue()).isBefore(LocalDateTime.now().minusDays(29));
    }

    @Test
    @DisplayName("탈퇴 후 30일이 지나지 않은 회원은 삭제하지 않는다")
    void doesNotDeleteRecentlyWithdrawnMembers() {
        when(memberRepository.findByDeletedAtBefore(any())).thenReturn(List.of());

        scheduler.deleteWithdrawnMembers();

        verify(memberRepository, never()).deleteWithdrawnMembers(any());
    }
}