package com.unplan.unplanserver.domain.onboarding.repository;

import com.unplan.unplanserver.domain.onboarding.entity.SleepCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SleepConditionRepository extends JpaRepository<SleepCondition, Long> {

    Optional<SleepCondition> findByMemberId(Long memberId);
}