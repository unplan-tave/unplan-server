package com.unplan.unplanserver.domain.schedule;

import com.unplan.unplanserver.domain.schedule.entity.LocationInfo;
import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.LocationInfoRepository;
import com.unplan.unplanserver.domain.schedule.repository.RecurrenceRuleRepository;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 일정 삭제 시 schedule_id 를 FK 로 참조하는 recurrence_rule·location_info 행이
 * 함께 정리되는지 검증한다(#120). 삭제 SQL 은 flush 시점에 실행되므로, 트랜잭션 롤백으로
 * 테스트 데이터를 격리하면서도 em.flush() 로 FK 제약 위반을 즉시 드러나게 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleDeleteIntegrationTest {

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;
    @Autowired private LocationInfoRepository locationInfoRepository;
    @PersistenceContext private EntityManager em;

    @Test
    @DisplayName("반복 일정 삭제 시 recurrence_rule 행이 함께 삭제된다")
    void deleteRecurringSchedule() {
        Schedule s = saveSchedule(9100L, "반복삭제", true);
        recurrenceRuleRepository.save(RecurrenceRule.builder()
                .schedule(s).freq(RecurrenceFreq.DAILY).interval(1).build());
        // 운영의 삭제 요청은 새 영속성 컨텍스트에서 시작하므로 셋업 엔티티를 비워 동일 조건으로 만든다
        em.flush();
        em.clear();

        scheduleService.deleteSchedule(9100L, s.getScheduleId());
        em.flush();
        em.clear();

        assertThat(scheduleRepository.findById(s.getScheduleId())).isEmpty();
        assertThat(recurrenceRuleRepository.findByScheduleIn(List.of(s))).isEmpty();
    }

    @Test
    @DisplayName("위치 정보가 있는 일정 삭제 시 location_info 행이 함께 삭제된다")
    void deleteScheduleWithLocation() {
        Schedule s = saveSchedule(9101L, "위치삭제", false);
        locationInfoRepository.save(LocationInfo.builder()
                .schedule(s).latitude(BigDecimal.ONE).longitude(BigDecimal.ONE).build());
        // 운영의 삭제 요청은 새 영속성 컨텍스트에서 시작하므로 셋업 엔티티를 비워 동일 조건으로 만든다
        em.flush();
        em.clear();

        scheduleService.deleteSchedule(9101L, s.getScheduleId());
        em.flush();
        em.clear();

        assertThat(scheduleRepository.findById(s.getScheduleId())).isEmpty();
        assertThat(locationInfoRepository.findBySchedule(s)).isEmpty();
    }

    private Schedule saveSchedule(Long memberId, String title, boolean recurring) {
        return scheduleRepository.save(Schedule.builder()
                .memberId(memberId).title(title).conditionTag(ConditionTag.CORE_TASK)
                .date(LocalDate.of(2026, 6, 1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .isQueue(false).isRecurring(recurring).isConflict(false)
                .status(ScheduleStatus.TODO).build());
    }
}
