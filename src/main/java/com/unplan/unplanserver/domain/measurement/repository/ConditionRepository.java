package com.unplan.unplanserver.domain.measurement.repository;

import com.unplan.unplanserver.domain.measurement.entity.Condition;
import com.unplan.unplanserver.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ConditionRepository extends JpaRepository<Condition, Long> {

    List<Condition> findAllByMemberAndMeasuredAtBetween(
            Member member,
            LocalDateTime start,
            LocalDateTime end
    );

    Optional<Condition> findByConditionIdAndMemberMemberId(
            Long conditionId,
            Long memberId
    );
}