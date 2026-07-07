package com.unplan.unplanserver.domain.recommendation.repository;

import com.unplan.unplanserver.domain.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    // 수락 시 본인 소유 검증과 함께 조회
    Optional<Recommendation> findByRecommendIdAndMemberId(Long recommendId, Long memberId);

    // 재생성 시 이전 노출분(미수락분) 정리. 수락 이력(acceptedScheduleId != null)은 남긴다.
    // 파생 delete(건별 SELECT 후 DELETE) 대신 벌크 DELETE 한 방으로 처리.
    @Modifying(clearAutomatically = true)
    @Query("delete from Recommendation r " +
            "where r.memberId = :memberId and r.date = :date and r.acceptedScheduleId is null")
    void deleteByMemberIdAndDateAndAcceptedScheduleIdIsNull(@Param("memberId") Long memberId,
                                                            @Param("date") LocalDate date);

    // 큐카드 7일 추천 재생성 시, 해당 큐카드의 이전 노출분(미수락분)을 날짜 무관하게 정리.
    // 수락 이력(acceptedScheduleId != null)은 남긴다.
    @Modifying(clearAutomatically = true)
    @Query("delete from Recommendation r " +
            "where r.memberId = :memberId and r.sourceScheduleId = :sourceScheduleId " +
            "and r.acceptedScheduleId is null")
    void deleteByMemberIdAndSourceScheduleIdAndAcceptedScheduleIdIsNull(@Param("memberId") Long memberId,
                                                                        @Param("sourceScheduleId") Long sourceScheduleId);
}
