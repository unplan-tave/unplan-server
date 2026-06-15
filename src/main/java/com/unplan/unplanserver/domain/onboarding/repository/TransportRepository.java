package com.unplan.unplanserver.domain.onboarding.repository;

import com.unplan.unplanserver.domain.onboarding.entity.Transport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransportRepository extends JpaRepository<Transport, Long> {

    List<Transport> findAllByMemberId(Long memberId);

    @Modifying
    @Query("delete from Transport t where t.memberId = :memberId")
    void deleteAllByMemberId(@Param("memberId") Long memberId);
}