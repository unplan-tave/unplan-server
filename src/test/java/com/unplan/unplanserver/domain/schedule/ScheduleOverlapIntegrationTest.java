package com.unplan.unplanserver.domain.schedule;

import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.RecurrenceRuleRepository;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 핀 카드 시간 겹침 차단(validatePinNotOverlapping) 통합 검증.
 * 실제 JPA/H2로 반복 인스턴스 확장까지 태워야 하므로, private 메서드를 리플렉션으로 직접 호출한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleOverlapIntegrationTest {

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;

    private static final Long MEMBER_ID = 7777L;
    private static final LocalDate D = LocalDate.of(2026, 6, 15);

    @Test
    @DisplayName("같은 날짜에 1분이라도 겹치는 핀 카드가 있으면 TIME_RANGE_OVERLAP")
    void overlappingPinBlocked() {
        savePin(MEMBER_ID, D, "10:00", "11:00");

        assertThatThrownBy(() -> validate(MEMBER_ID, D, "10:30", "11:30", null))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                        ((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.TIME_RANGE_OVERLAP));

        // 1분 겹침(10:59~)도 차단
        assertThatThrownBy(() -> validate(MEMBER_ID, D, "10:59", "11:30", null))
                .isInstanceOf(CustomException.class);
    }

    @Test
    @DisplayName("경계가 맞닿기만 하면(10:00~11:00 과 11:00~12:00) 겹침이 아니다")
    void touchingBoundaryAllowed() {
        savePin(MEMBER_ID, D, "10:00", "11:00");

        assertThatCode(() -> validate(MEMBER_ID, D, "11:00", "12:00", null)).doesNotThrowAnyException();
        assertThatCode(() -> validate(MEMBER_ID, D, "09:00", "10:00", null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("반복 일정의 해당 날짜 인스턴스와 겹쳐도 차단된다")
    void overlappingRecurringInstanceBlocked() {
        // 하루 전(D-1)에 시작하는 매일 반복 핀 → D 에 14:00~15:00 인스턴스가 생김
        Schedule recurring = scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID).title("반복핀").conditionTag(ConditionTag.CORE_TASK)
                .date(D.minusDays(1)).startTime(LocalTime.parse("14:00")).endTime(LocalTime.parse("15:00"))
                .isQueue(false).isRecurring(true).isConflict(false)
                .status(ScheduleStatus.TODO).build());
        recurrenceRuleRepository.save(RecurrenceRule.builder()
                .schedule(recurring).freq(RecurrenceFreq.DAILY).interval(1).build());

        assertThatThrownBy(() -> validate(MEMBER_ID, D, "14:30", "15:30", null))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> org.assertj.core.api.Assertions.assertThat(
                        ((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.TIME_RANGE_OVERLAP));
    }

    @Test
    @DisplayName("큐 카드(시간 미지정)는 겹침 대상이 아니다")
    void queueCardIgnored() {
        Schedule queue = Schedule.builder()
                .memberId(MEMBER_ID).title("큐").date(D)
                .isQueue(true).isRecurring(false).isConflict(false)
                .status(ScheduleStatus.TODO).build();
        scheduleRepository.save(queue);

        assertThatCode(() -> validate(MEMBER_ID, D, "10:00", "11:00", null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("수정 시 자기 자신은 겹침 대상에서 제외된다")
    void excludesSelfOnUpdate() {
        Schedule pin = savePin(MEMBER_ID, D, "16:00", "17:00");

        // 동일 시간 그대로 두어도 자기 자신이므로 통과
        assertThatCode(() -> validate(MEMBER_ID, D, "16:00", "17:00", pin.getScheduleId()))
                .doesNotThrowAnyException();
    }

    // ─────────────────────────── 헬퍼 ───────────────────────────

    private Schedule savePin(Long memberId, LocalDate date, String start, String end) {
        return scheduleRepository.save(Schedule.builder()
                .memberId(memberId).title("핀").conditionTag(ConditionTag.CORE_TASK)
                .date(date).startTime(LocalTime.parse(start)).endTime(LocalTime.parse(end))
                .isQueue(false).isRecurring(false).isConflict(false)
                .status(ScheduleStatus.TODO).build());
    }

    /** private validatePinNotOverlapping 을 리플렉션으로 호출하고 원인 예외를 그대로 던진다. */
    private void validate(Long memberId, LocalDate date, String start, String end, Long excludeId) {
        try {
            Method m = ScheduleService.class.getDeclaredMethod("validatePinNotOverlapping",
                    Long.class, LocalDate.class, LocalTime.class, LocalTime.class, Long.class);
            m.setAccessible(true);
            m.invoke(scheduleService, memberId, date, LocalTime.parse(start), LocalTime.parse(end), excludeId);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException re) throw re;
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
