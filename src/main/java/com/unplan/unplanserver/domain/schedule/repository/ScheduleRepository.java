package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByMemberIdAndDate(Long memberId, LocalDate date);

    java.util.Optional<Schedule> findByScheduleIdAndMemberId(Long scheduleId, Long memberId);

    List<Schedule> findByMemberIdAndDateBetween(Long memberId, LocalDate startDate, LocalDate endDate);

    // 반복 인스턴스는 원본 날짜 이후에만 생기므로, 조회 범위 끝(rangeEnd)보다 늦게 시작하는 원본은 제외.
    List<Schedule> findByMemberIdAndIsRecurringTrueAndDateLessThanEqual(Long memberId, LocalDate date);
}