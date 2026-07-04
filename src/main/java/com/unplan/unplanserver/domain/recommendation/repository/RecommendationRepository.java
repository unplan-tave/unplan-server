package com.unplan.unplanserver.domain.recommendation.repository;

import com.unplan.unplanserver.domain.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    // 수락 시 본인 소유 검증과 함께 조회
    Optional<Recommendation> findByRecommendIdAndMemberId(Long recommendId, Long memberId);

    // 재생성 시 이전 노출분(미수락분) 정리. 수락 이력(acceptedScheduleId != null)은 남긴다
    void deleteByMemberIdAndDateAndAcceptedScheduleIdIsNull(Long memberId, LocalDate date);
}
