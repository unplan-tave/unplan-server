package com.unplan.unplanserver.domain.onboarding.repository;

import com.unplan.unplanserver.domain.onboarding.entity.Transport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransportRepository extends JpaRepository<Transport, Long> {

    List<Transport> findAllByMemberId(Long memberId);

    void deleteAllByMemberId(Long memberId);
}