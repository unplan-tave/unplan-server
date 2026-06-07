package com.unplan.unplanserver.domain.jwt.repository;

import com.unplan.unplanserver.domain.jwt.entity.Refresh;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshRepository extends JpaRepository<Refresh, Long> {

    void deleteByMemberIdAndDeviceId(Long memberId, String s);
}
