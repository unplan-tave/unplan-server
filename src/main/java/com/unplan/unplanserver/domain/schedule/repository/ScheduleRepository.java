package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {

    List<Schedule> findByMemberIdAndDate(Long memberId, LocalDate date);

    java.util.Optional<Schedule> findByScheduleIdAndMemberId(Long scheduleId, Long memberId);

    List<Schedule> findByMemberIdAndDateBetween(Long memberId, LocalDate startDate, LocalDate endDate);

    // 반복 인스턴스는 원본 날짜 이후에만 생기므로, 조회 범위 끝(rangeEnd)보다 늦게 시작하는 원본은 제외.
    List<Schedule> findByMemberIdAndIsRecurringTrueAndDateLessThanEqual(Long memberId, LocalDate date);

    // 추천 후보 큐 카드: 완료(DONE)·소요시간 미정은 DB에서 걸러 조회 (불필요한 전체 조회 방지)
    @Query("select s from Schedule s " +
            "where s.memberId = :memberId and s.isQueue = true " +
            "and s.status <> com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus.DONE " +
            "and s.estimatedTime is not null")
    List<Schedule> findActiveQueueCards(@Param("memberId") Long memberId);
}