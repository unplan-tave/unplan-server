package com.unplan.unplanserver.domain.recommendation.repository;

import com.unplan.unplanserver.domain.recommendation.entity.RecommendationPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RecommendationPassRepository extends JpaRepository<RecommendationPass, Long> {

    // 같은 (member, date, 큐카드) 패스가 이미 있으면 중복 저장하지 않기 위한 멱등 체크
    boolean existsByMemberIdAndDateAndSourceScheduleId(Long memberId, LocalDate date, Long sourceScheduleId);

    // 해당 날짜에 패스된 큐 카드 id 목록 — 추천 재생성 시 후보에서 제외하는 데 사용
    @Query("select rp.sourceScheduleId from RecommendationPass rp " +
            "where rp.memberId = :memberId and rp.date = :date")
    List<Long> findSourceScheduleIdsByMemberIdAndDate(@Param("memberId") Long memberId,
                                                      @Param("date") LocalDate date);
}
