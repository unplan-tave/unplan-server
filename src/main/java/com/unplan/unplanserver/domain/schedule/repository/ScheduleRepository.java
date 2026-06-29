package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByMemberIdAndDate(Long memberId, LocalDate date);

    java.util.Optional<Schedule> findByScheduleIdAndMemberId(Long scheduleId, Long memberId);

    List<Schedule> findByMemberIdAndDateBetween(Long memberId, LocalDate startDate, LocalDate endDate);

    List<Schedule> findByMemberIdAndIsRecurringTrue(Long memberId);
}