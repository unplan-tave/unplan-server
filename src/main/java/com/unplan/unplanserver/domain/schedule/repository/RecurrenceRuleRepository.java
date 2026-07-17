package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecurrenceRuleRepository extends JpaRepository<RecurrenceRule, Long> {

    List<RecurrenceRule> findByScheduleIn(List<Schedule> schedules);

    void deleteBySchedule(Schedule schedule);
}
