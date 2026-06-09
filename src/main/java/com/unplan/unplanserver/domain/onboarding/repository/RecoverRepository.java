package com.unplan.unplanserver.domain.onboarding.repository;

import com.unplan.unplanserver.domain.onboarding.entity.RecoverEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecoverRepository extends JpaRepository<RecoverEntity, Long> {

    List<RecoverEntity> findByMemberId(Long memberId);

    @Modifying
    @Query("delete from RecoverEntity r " +
            "where r.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}