package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.entity.SchedulePersonalTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchedulePersonalTagRepository extends JpaRepository<SchedulePersonalTag, Long> {

    List<SchedulePersonalTag> findBySchedule(Schedule schedule);
}
