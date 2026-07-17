package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {

    List<RecurrenceRule> findByScheduleIn(List<Schedule> schedules);

    // 조회 범위에 인스턴스가 생길 수 있는 '활성' 반복 규칙을 원본 일정과 함께 단일 쿼리로 조회한다.
    // 시작일 <= rangeEnd(그 뒤 시작은 인스턴스가 범위 밖) AND 아직 종료 안 됨(until null 이거나 >= rangeStart).
    // 종료된 반복까지 로드 후 메모리에서 거르던 것을 DB 단계로 옮겨 데이터 누적 시 부하를 줄인다(#114).
    @Query("select r from RecurrenceRule r join fetch r.schedule s " +
            "where s.memberId = :memberId and s.isRecurring = true " +
            "and s.date <= :rangeEnd and (r.until is null or r.until >= :rangeStart)")
    List<RecurrenceRule> findActiveRulesWithSchedule(@Param("memberId") Long memberId,
                                                     @Param("rangeStart") LocalDate rangeStart,
                                                     @Param("rangeEnd") LocalDate rangeEnd);

    // 파생 삭제(select 후 건별 delete) 대신 벌크 삭제로 한 번에 처리
    @Modifying
    @Query("delete from RecurrenceRule r where r.schedule = :schedule")
    void deleteBySchedule(@Param("schedule") Schedule schedule);
}
