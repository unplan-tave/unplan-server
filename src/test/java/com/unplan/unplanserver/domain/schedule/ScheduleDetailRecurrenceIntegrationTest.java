package com.unplan.unplanserver.domain.schedule;

import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleDetailResponse;
import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.RecurrenceRuleRepository;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 일정 상세 조회 응답이 반복 규칙(recurrence) 상세를 담는지 검증한다.
 * 프론트가 반복 설정 화면(주기·요일·종료일 등)을 그리려면 is_recurring 여부만으로는 부족하다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleDetailRecurrenceIntegrationTest {

    private static final Long MEMBER_ID = 9300L;

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;

    @Test
    @DisplayName("반복 일정 상세 조회는 recurrence 규칙(freq/interval/byDay/until 등)을 담는다")
    void detailIncludesRecurrenceRule() {
        Schedule s = saveSchedule(true);
        recurrenceRuleRepository.save(RecurrenceRule.builder()
                .schedule(s)
                .freq(RecurrenceFreq.WEEKLY)
                .interval(2)
                .byDay("MON,WED")
                .until(LocalDate.of(2026, 12, 31))
                .count(10)
                .build());

        ScheduleDetailResponse res = scheduleService.getScheduleDetail(MEMBER_ID, s.getScheduleId());

        assertThat(res.getIsRecurring()).isTrue();
        assertThat(res.getRecurrence()).isNotNull();
        assertThat(res.getRecurrence().getFreq()).isEqualTo(RecurrenceFreq.WEEKLY);
        assertThat(res.getRecurrence().getInterval()).isEqualTo(2);
        assertThat(res.getRecurrence().getByDay()).isEqualTo("MON,WED");
        assertThat(res.getRecurrence().getUntil()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(res.getRecurrence().getCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("반복이 아닌 일정 상세 조회는 recurrence 가 null 이다")
    void detailWithoutRecurrenceIsNull() {
        Schedule s = saveSchedule(false);

        ScheduleDetailResponse res = scheduleService.getScheduleDetail(MEMBER_ID, s.getScheduleId());

        assertThat(res.getIsRecurring()).isFalse();
        assertThat(res.getRecurrence()).isNull();
    }

    private Schedule saveSchedule(boolean recurring) {
        return scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID).title("일정").conditionTag(ConditionTag.CORE_TASK)
                .date(LocalDate.of(2026, 6, 1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .isQueue(false).isRecurring(recurring).isConflict(false)
                .status(ScheduleStatus.TODO).build());
    }
}
