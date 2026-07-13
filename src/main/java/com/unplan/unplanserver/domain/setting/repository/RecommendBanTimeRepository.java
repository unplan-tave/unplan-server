package com.unplan.unplanserver.domain.setting.repository;

import com.unplan.unplanserver.domain.setting.entity.RecommendBanTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendBanTimeRepository extends JpaRepository<RecommendBanTime, Long> {
    void deleteBySettingId(Long settingId);
}
