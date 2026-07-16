package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {

    List<Schedule> findByMemberIdAndDate(Long memberId, LocalDate date);

    java.util.Optional<Schedule> findByScheduleIdAndMemberId(Long scheduleId, Long memberId);

    List<Schedule> findByMemberIdAndDateBetween(Long memberId, LocalDate startDate, LocalDate endDate);

    // 조회 범위에 인스턴스가 생길 수 있는 '활성' 반복 원본만 조회한다.
    // 시작일 <= rangeEnd(그 뒤 시작은 인스턴스가 범위 밖) AND 아직 종료 안 됨(until null 이거나 >= rangeStart).
    // 종료된 반복까지 로드 후 메모리에서 거르던 것을 DB 단계로 옮겨 데이터 누적 시 부하를 줄인다(#114).
    @Query("select s from Schedule s join RecurrenceRule r on r.schedule = s " +
            "where s.memberId = :memberId and s.isRecurring = true " +
            "and s.date <= :rangeEnd and (r.until is null or r.until >= :rangeStart)")
    List<Schedule> findActiveRecurringSchedules(@Param("memberId") Long memberId,
                                                @Param("rangeStart") LocalDate rangeStart,
                                                @Param("rangeEnd") LocalDate rangeEnd);

    // 추천 후보 큐 카드: 완료(DONE)·소요시간 미정은 DB에서 걸러 조회 (불필요한 전체 조회 방지)
    @Query("select s from Schedule s " +
            "where s.memberId = :memberId and s.isQueue = true " +
            "and s.status <> com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus.DONE " +
            "and s.estimatedTime is not null")
    List<Schedule> findActiveQueueCards(@Param("memberId") Long memberId);
}