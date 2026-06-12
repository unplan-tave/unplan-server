package com.unplan.unplanserver.domain.member.repository;

import com.unplan.unplanserver.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByOauthId(Long oauthId);

    Optional<Member> findByOauthId(Long oauthId);
}
