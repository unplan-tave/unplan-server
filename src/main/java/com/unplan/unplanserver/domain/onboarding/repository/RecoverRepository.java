package com.unplan.unplanserver.domain.onboarding.repository;

import com.unplan.unplanserver.domain.onboarding.entity.Recover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecoverRepository extends JpaRepository<Recover, Long> {

    List<Recover> findByMemberId(Long memberId);

    // 저장 순서(기본 방법 먼저, 이후 직접 입력) 보존 — 추천 회복 수단 노출/선택 순서에 사용
    List<Recover> findByMemberIdOrderByIdAsc(Long memberId);

    @Modifying
    @Query("delete from Recover r " +
            "where r.memberId = :memberId")
    void deleteByMemberId(@Param("memberId") Long memberId);
}