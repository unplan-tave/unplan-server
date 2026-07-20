package com.unplan.unplanserver.domain.schedule;

import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleGetResponse;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.repository.RecurrenceRuleRepository;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PeriodScheduleIntegrationTest {

    private static final Long MEMBER_ID = 9901L;
    private static final LocalDate START = LocalDate.of(2026, 7, 20);
    private static final LocalDate END = LocalDate.of(2026, 7, 22);

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;

    @Test
    @DisplayName("7/20~7/22 기간 핀 카드는 시작일·중간일·종료일에 보이고 다음 날에는 보이지 않는다")
    void periodPinVisibleOnEveryInclusiveDate() {
        savePeriodPin("기간 일정", START, END);

        assertThat(titlesOn(START)).contains("기간 일정");
        assertThat(titlesOn(START.plusDays(1))).contains("기간 일정");
        assertThat(titlesOn(END)).contains("기간 일정");
        assertThat(titlesOn(END.plusDays(1))).doesNotContain("기간 일정");

        ScheduleGetResponse middle = scheduleService.getSchedulesByDate(MEMBER_ID, START.plusDays(1)).get(0);
        assertThat(middle.getDate()).isEqualTo(START.toString());
        assertThat(middle.getEndDate()).isEqualTo(END.toString());
    }

    @Test
    @DisplayName("endDate가 null인 단일 날짜 일정은 기존처럼 시작 날짜에만 보인다")
    void nullEndDateRemainsSingleDay() {
        savePeriodPin("단일 일정", START, null);

        assertThat(titlesOn(START)).contains("단일 일정");
        assertThat(titlesOn(START.plusDays(1))).doesNotContain("단일 일정");
    }

    @Test
    @DisplayName("주간·월간 조회는 기간 핀 카드를 범위 내 각 날짜에 포함한다")
    void weeklyAndMonthlyExpandPeriodAcrossDays() {
        Schedule period = savePeriodPin("기간 일정", START, END);

        var weekly = scheduleService.getSchedulesByWeek(MEMBER_ID, START);
        assertThat(weekly.getWeeklySchedules())
                .filteredOn(day -> List.of("2026-07-20", "2026-07-21", "2026-07-22").contains(day.getDate()))
                .allSatisfy(day -> assertThat(day.getSchedules())
                        .extracting(s -> s.getScheduleId()).contains(period.getScheduleId()));

        var monthly = scheduleService.getSchedulesByMonth(MEMBER_ID, YearMonth.of(2026, 7));
        assertThat(monthly.getSchedules())
                .filteredOn(day -> List.of("2026-07-20", "2026-07-21", "2026-07-22").contains(day.getDate()))
                .hasSize(3)
                .allSatisfy(day -> assertThat(day.getCount()).isEqualTo(1));
    }

    @Test
    @DisplayName("기간 반복 일정은 각 발생일의 기간만큼 노출되고 같은 날짜에 중복되지 않는다")
    void recurringPeriodExpandsWithoutDuplicateCollision() {
        Schedule recurring = scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID).title("주간 기간 반복").conditionTag(ConditionTag.CORE_TASK)
                .date(START).endDate(END)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .isQueue(false).isRecurring(true).isConflict(false)
                .status(ScheduleStatus.TODO).build());
        recurrenceRuleRepository.save(RecurrenceRule.builder()
                .schedule(recurring).freq(RecurrenceFreq.WEEKLY).interval(1).build());

        LocalDate nextOccurrenceMiddle = START.plusWeeks(1).plusDays(1);
        assertThat(titlesOn(nextOccurrenceMiddle))
                .containsExactly("주간 기간 반복");

        var weekly = scheduleService.getSchedulesByWeek(MEMBER_ID, nextOccurrenceMiddle);
        assertThat(weekly.getWeeklySchedules().stream()
                .flatMap(day -> day.getSchedules().stream())
                .filter(item -> item.getScheduleId().equals(recurring.getScheduleId())))
                .hasSize(3);
    }

    @Test
    @DisplayName("생성·수정 시 endDate가 date보다 이전이면 실패한다")
    void reversedDateRangeRejectedOnCreateAndUpdate() {
        ScheduleCreateRequest create = new ScheduleCreateRequest();
        set(create, "title", "잘못된 기간");
        set(create, "conditionTag", ConditionTag.CORE_TASK);
        set(create, "date", START);
        set(create, "endDate", START.minusDays(1));
        set(create, "startTime", LocalTime.of(9, 0));
        set(create, "endTime", LocalTime.of(10, 0));
        set(create, "isRemindOn", false);

        assertThatThrownBy(() -> scheduleService.createSchedule(MEMBER_ID, create))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SCHEDULE_DATE_RANGE);

        Schedule existing = savePeriodPin("수정 대상", START, null);
        ScheduleUpdateRequest update = new ScheduleUpdateRequest();
        update.setEndDate(START.minusDays(1));

        assertThatThrownBy(() -> scheduleService.updateSchedule(MEMBER_ID, existing.getScheduleId(), update))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_SCHEDULE_DATE_RANGE);
    }

    @Test
    @DisplayName("수정 요청의 명시적 endDate null은 기간 일정을 단일 날짜 일정으로 변경한다")
    void explicitNullEndDateClearsPeriod() {
        Schedule period = savePeriodPin("기간 해제", START, END);
        ScheduleUpdateRequest update = new ScheduleUpdateRequest();
        update.setEndDate(null);

        scheduleService.updateSchedule(MEMBER_ID, period.getScheduleId(), update);

        assertThat(scheduleRepository.findById(period.getScheduleId()).orElseThrow().getEndDate()).isNull();
        assertThat(titlesOn(START.plusDays(1))).doesNotContain("기간 해제");
    }

    private List<String> titlesOn(LocalDate date) {
        return scheduleService.getSchedulesByDate(MEMBER_ID, date).stream()
                .map(ScheduleGetResponse::getTitle)
                .toList();
    }

    private Schedule savePeriodPin(String title, LocalDate date, LocalDate endDate) {
        return scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID)
                .title(title)
                .conditionTag(ConditionTag.CORE_TASK)
                .date(date)
                .endDate(endDate)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .isQueue(false)
                .isRecurring(false)
                .isConflict(false)
                .status(ScheduleStatus.TODO)
                .build());
    }

    private void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
