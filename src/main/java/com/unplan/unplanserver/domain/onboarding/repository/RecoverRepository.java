package com.unplan.unplanserver.domain.onboarding.repository;

import com.unplan.unplanserver.domain.onboarding.entity.RecoverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecoverRepository extends JpaRepository<RecoverEntity, Long> {

    List<RecoverEntity> findByMemberId(Long memberId);

    void deleteByMemberId(Long memberId);
}