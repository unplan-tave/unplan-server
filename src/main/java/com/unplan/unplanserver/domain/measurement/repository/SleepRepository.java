package com.unplan.unplanserver.domain.measurement.repository;

import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import com.unplan.unplanserver.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SleepRepository extends JpaRepository<Sleep, Long> {

    Optional<Sleep> findBySleepIdAndMember(Long sleepId, Member member);
}