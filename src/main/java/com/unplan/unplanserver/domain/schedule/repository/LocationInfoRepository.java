package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.LocationInfo;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LocationInfoRepository extends JpaRepository<LocationInfo, Long> {
    Optional<LocationInfo> findBySchedule(Schedule schedule);

    // 파생 삭제(select 후 건별 delete) 대신 벌크 삭제로 한 번에 처리
    @Modifying
    @Query("delete from LocationInfo l where l.schedule = :schedule")
    void deleteBySchedule(@Param("schedule") Schedule schedule);
}
