package com.unplan.unplanserver.domain.onboarding.repository;

import com.unplan.unplanserver.domain.onboarding.entity.Biorhythm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BiorhythmRepository extends JpaRepository<Biorhythm, Long> {

    Optional<Biorhythm> findByMemberId(Long memberId);
}