package com.unplan.unplanserver.domain.memo.repository;

import com.unplan.unplanserver.domain.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    // 특정 유저의 특정 날짜 메모 개수 확인
    int countByMemberIdAndDate(Long memberId, LocalDate date);

    //메모 다중 삭제
    void deleteByDailyMemoIdIn(List<Long> memoIds);

    List<Memo> findByMemberIdAndDate(Long memberId, LocalDate date);

    List<Memo> findAllByDailyMemoIdInAndMemberId(List<Long> memoIds, Long memberId);
}