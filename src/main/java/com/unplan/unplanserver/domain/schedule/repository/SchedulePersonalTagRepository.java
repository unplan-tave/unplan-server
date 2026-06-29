package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.entity.SchedulePersonalTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchedulePersonalTagRepository extends JpaRepository<SchedulePersonalTag, Long> {

    List<SchedulePersonalTag> findBySchedule(Schedule schedule);

    // 일정 삭제 시 해당 일정의 태그 연결을 함께 제거 (FK 제약 위반·고아 행 방지)
    void deleteBySchedule(Schedule schedule);
}
