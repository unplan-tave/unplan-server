package com.unplan.unplanserver.domain.schedule;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 반복 확장 활성-필터(#114) 통합 검증 — findActiveRecurringSchedules 로 종료된(until 지난) 반복이
 * DB 단계에서 제외되는지, 활성 반복은 여전히 인스턴스가 생기는지 실제 JPA/H2 로 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RecurringExpansionIntegrationTest {

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;

    private static final Long MEMBER_ID = 8500L;
    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @Test
    @DisplayName("종료된(until<조회일) 반복은 인스턴스가 생기지 않고, 활성 반복은 생긴다")
    void endedRecurringExcludedActiveIncluded() {
        // 활성: 매일 반복, 종료 없음 → 오늘 인스턴스 생김
        dailyRecurring("활성반복", TODAY.minusDays(3), null);
        // 활성: 매일 반복, until 이 오늘 이후 → 오늘 인스턴스 생김
        dailyRecurring("활성반복-until미래", TODAY.minusDays(3), TODAY.plusDays(5));
        // 종료: 매일 반복이지만 until 이 어제 → 오늘 인스턴스 없음 (쿼리에서 제외)
        dailyRecurring("종료된반복", TODAY.minusDays(10), TODAY.minusDays(1));

        List<TitleOnly> onDay = scheduleService.findSchedulesWithRecurring(MEMBER_ID, TODAY).stream()
                .map(s -> new TitleOnly(s.getTitle()))
                .toList();

        assertThat(onDay).extracting(TitleOnly::title)
                .contains("활성반복", "활성반복-until미래")
                .doesNotContain("종료된반복");
    }

    private record TitleOnly(String title) {}

    private void dailyRecurring(String title, LocalDate originalDate, LocalDate until) {
        Schedule s = scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID).title(title).conditionTag(ConditionTag.CORE_TASK)
                .date(originalDate).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .isQueue(false).isRecurring(true).isConflict(false)
                .status(ScheduleStatus.TODO).build());
        recurrenceRuleRepository.save(RecurrenceRule.builder()
                .schedule(s).freq(RecurrenceFreq.DAILY).interval(1).until(until).build());
    }
}
