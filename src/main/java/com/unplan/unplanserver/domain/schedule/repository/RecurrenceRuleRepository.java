package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {

    List<RecurrenceRule> findByScheduleIn(List<Schedule> schedules);

    // 상세 조회 시 해당 일정의 반복 규칙(단건, schedule 과 1:1)을 조회. 반복 없으면 empty.
    Optional<RecurrenceRule> findBySchedule(Schedule schedule);

    // 조회 종료일 이전에 시작한 반복 규칙을 원본 일정과 함께 조회한다.
    // 기간 반복 일정은 마지막 발생 시작일(until)이 조회 시작일보다 앞서도 그 인스턴스의 endDate가
    // 조회 범위와 겹칠 수 있으므로 until 하한 필터는 서비스에서 기간 길이까지 고려해 적용한다.
    @Query("select r from RecurrenceRule r join fetch r.schedule s " +
            "where s.memberId = :memberId and s.isRecurring = true " +
            "and s.date <= :rangeEnd")
    List<RecurrenceRule> findActiveRulesWithSchedule(@Param("memberId") Long memberId,
                                                     @Param("rangeStart") LocalDate rangeStart,
                                                     @Param("rangeEnd") LocalDate rangeEnd);

    // 파생 삭제(select 후 건별 delete) 대신 벌크 삭제로 한 번에 처리
    @Modifying
    @Query("delete from RecurrenceRule r where r.schedule = :schedule")
    void deleteBySchedule(@Param("schedule") Schedule schedule);
}
