package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.entity.SchedulePersonalTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SchedulePersonalTagRepository extends JpaRepository<SchedulePersonalTag, Long> {

    List<SchedulePersonalTag> findBySchedule(Schedule schedule);

    // 일정에 연결된 태그 이름만 단일 JOIN 쿼리로 조회 (LAZY personalTag 를 건별 로딩하는 N+1 방지)
    @Query("select spt.personalTag.name from SchedulePersonalTag spt where spt.schedule = :schedule")
    List<String> findTagNamesBySchedule(@Param("schedule") Schedule schedule);

    // 여러 일정의 (일정id, 태그이름)을 한 번에 조회 (검색 결과 페이지의 태그 배치 매핑용, N+1 방지)
    @Query("select spt.schedule.scheduleId as scheduleId, spt.personalTag.name as tagName " +
            "from SchedulePersonalTag spt where spt.schedule.scheduleId in :scheduleIds")
    List<ScheduleTagRow> findTagRowsByScheduleIds(@Param("scheduleIds") Collection<Long> scheduleIds);

    interface ScheduleTagRow {
        Long getScheduleId();
        String getTagName();
    }

    // 일정 삭제 시 해당 일정의 태그 연결을 함께 제거 (FK 제약 위반·고아 행 방지)
    void deleteBySchedule(Schedule schedule);
}
