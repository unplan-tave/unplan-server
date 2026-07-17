package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.LocationInfo;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationInfoRepository extends JpaRepository<LocationInfo, Long> {
    Optional<LocationInfo> findBySchedule(Schedule schedule);

    void deleteBySchedule(Schedule schedule);
}
