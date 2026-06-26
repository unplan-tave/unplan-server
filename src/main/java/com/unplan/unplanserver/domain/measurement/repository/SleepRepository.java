package com.unplan.unplanserver.domain.measurement.repository;

import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SleepRepository extends JpaRepository<Sleep, Long> {

    Optional<Sleep> findBySleepIdAndMemberMemberId(Long sleepId, Long memberId);
}