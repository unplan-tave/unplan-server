package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {

    List<RecurrenceRule> findByScheduleIn(List<Schedule> schedules);

    // 파생 삭제(select 후 건별 delete) 대신 벌크 삭제로 한 번에 처리
    @Modifying
    @Query("delete from RecurrenceRule r where r.schedule = :schedule")
    void deleteBySchedule(@Param("schedule") Schedule schedule);
}
