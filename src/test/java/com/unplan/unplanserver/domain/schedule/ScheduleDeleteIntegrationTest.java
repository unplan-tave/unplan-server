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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 일정 삭제 시 schedule_id 를 FK 로 참조하는 recurrence_rule·location_info 행이
 * 함께 정리되는지 검증한다(#120). 트랜잭션 롤백 없이 실제 flush 를 거쳐 FK 위반을 잡아야 하므로
 * 클래스 레벨 @Transactional 을 걸지 않는다.
 */
@SpringBootTest
@ActiveProfiles("test")
class ScheduleDeleteIntegrationTest {

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;
    @Autowired private LocationInfoRepository locationInfoRepository;

    @Test
    @DisplayName("반복 일정 삭제 시 recurrence_rule 행이 함께 삭제된다")
    void deleteRecurringSchedule() {
        Schedule s = saveSchedule(9100L, "반복삭제", true);
        recurrenceRuleRepository.save(RecurrenceRule.builder()
                .schedule(s).freq(RecurrenceFreq.DAILY).interval(1).build());

        scheduleService.deleteSchedule(9100L, s.getScheduleId());

        assertThat(scheduleRepository.findById(s.getScheduleId())).isEmpty();
        assertThat(recurrenceRuleRepository.findByScheduleIn(java.util.List.of(s))).isEmpty();
    }

    @Test
    @DisplayName("위치 정보가 있는 일정 삭제 시 location_info 행이 함께 삭제된다")
    void deleteScheduleWithLocation() {
        Schedule s = saveSchedule(9101L, "위치삭제", false);
        locationInfoRepository.save(LocationInfo.builder()
                .schedule(s).latitude(BigDecimal.ONE).longitude(BigDecimal.ONE).build());

        scheduleService.deleteSchedule(9101L, s.getScheduleId());

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
