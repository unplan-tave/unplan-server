package com.unplan.unplanserver.domain.member.repository;

import com.unplan.unplanserver.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

}
