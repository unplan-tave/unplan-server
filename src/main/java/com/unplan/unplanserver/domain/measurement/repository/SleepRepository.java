package com.unplan.unplanserver.domain.measurement.repository;

import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SleepRepository extends JpaRepository<Sleep, Long> {

    Optional<Sleep> findBySleepIdAndMemberMemberId(Long sleepId, Long memberId);

    List<Sleep> findAllByMemberMemberIdAndContinuousSleepGroupId(Long memberId, String continuousSleepGroupId);

    List<Sleep> findAllByMemberMemberIdAndWakeUpTimeGreaterThanEqualAndWakeUpTimeLessThan(
            Long memberId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Sleep> findTop7ByMemberMemberIdAndWakeUpTimeBeforeOrderByWakeUpTimeDesc(
            Long memberId,
            LocalDateTime before
    );

    @Query("SELECT COUNT(s) > 0 FROM Sleep s " +
            "WHERE s.member.memberId = :memberId " +
            "AND :targetTime >= s.bedTime AND :targetTime < s.wakeUpTime")
    boolean existsByMemberIdAndSleepTimeOverlap(
            @Param("memberId") Long memberId,
            @Param("targetTime") LocalDateTime targetTime
    );
}
