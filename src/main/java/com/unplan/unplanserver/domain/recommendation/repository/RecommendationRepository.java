package com.unplan.unplanserver.domain.recommendation.repository;

import com.unplan.unplanserver.domain.recommendation.entity.Recommendation;
import com.unplan.unplanserver.domain.recommendation.enums.RecommendationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    // 특정 날짜의 노출 대상 추천 (목록 조회용)
    List<Recommendation> findByMemberIdAndDateAndStatus(Long memberId, LocalDate date, RecommendationStatus status);

    // 거절된 source 큐카드를 재계산 시 제외하기 위한 조회
    List<Recommendation> findByMemberIdAndDateAndStatusIn(Long memberId, LocalDate date, List<RecommendationStatus> statuses);

    // 수락/거절 시 본인 소유 검증과 함께 조회
    Optional<Recommendation> findByRecommendIdAndMemberId(Long recommendId, Long memberId);

    // 재생성 시 이전 노출분(PENDING) 정리. 수락/거절 이력은 남긴다
    void deleteByMemberIdAndDateAndStatus(Long memberId, LocalDate date, RecommendationStatus status);
}
