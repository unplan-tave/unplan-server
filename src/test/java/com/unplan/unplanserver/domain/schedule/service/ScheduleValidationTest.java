package com.unplan.unplanserver.domain.schedule.service;

import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.global.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 일정 저장 전 검증 로직(시작/종료 시간 쌍, N번째 요일 범위)의 경계값 검증.
 * DB/스프링 컨텍스트 없이 리플렉션으로 private 메서드를 직접 호출한다.
 */
class ScheduleValidationTest {

    private final ScheduleService service = new ScheduleService(null, null, null);

    private void validateTimePair(LocalTime start, LocalTime end) {
        invoke("validateTimePair", new Class<?>[]{LocalTime.class, LocalTime.class}, start, end);
    }

    private void validateRecurrence(RecurrenceFreq freq, String byDay) {
        try {
            ScheduleCreateRequest.RecurrenceRequest rec = new ScheduleCreateRequest.RecurrenceRequest();
            setField(rec, "freq", freq);
            setField(rec, "interval", 1);
            setField(rec, "byDay", byDay);
            invoke("validateRecurrence", new Class<?>[]{ScheduleCreateRequest.RecurrenceRequest.class}, rec);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void invoke(String name, Class<?>[] types, Object... args) {
        try {
            Method m = ScheduleService.class.getDeclaredMethod(name, types);
            m.setAccessible(true);
            m.invoke(service, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private LocalTime t(String s) { return LocalTime.parse(s); }

    // ─────────────────────────── 시작/종료 시간 쌍 ───────────────────────────

    @Test
    @DisplayName("둘 다 없음(큐 카드) / 정상 구간(핀 카드)은 통과")
    void validPairsPass() {
        assertDoesNotThrow(() -> validateTimePair(null, null));
        assertDoesNotThrow(() -> validateTimePair(t("09:00"), t("10:00")));
        assertDoesNotThrow(() -> validateTimePair(t("00:00"), t("23:59"))); // 하루 전체
    }

    @Test
    @DisplayName("한쪽만 있는 '반쪽 핀 카드'는 거부")
    void oneSidedTimeRejected() {
        assertThrows(CustomException.class, () -> validateTimePair(t("09:00"), null));
        assertThrows(CustomException.class, () -> validateTimePair(null, t("10:00")));
    }

    @Test
    @DisplayName("시작 >= 종료(역전·0분 구간)는 거부")
    void reversedOrZeroLengthRejected() {
        assertThrows(CustomException.class, () -> validateTimePair(t("10:00"), t("09:00")));
        assertThrows(CustomException.class, () -> validateTimePair(t("10:00"), t("10:00")));
    }

    // ─────────────────────────── MONTHLY N번째 요일 범위 ───────────────────────────

    @Test
    @DisplayName("N번째 요일은 1~5만 허용 — '0WED'/'6WED' 거부, '1WED'/'5WED' 통과")
    void nthWeekdayRange() {
        assertDoesNotThrow(() -> validateRecurrence(RecurrenceFreq.MONTHLY, "1WED"));
        assertDoesNotThrow(() -> validateRecurrence(RecurrenceFreq.MONTHLY, "5WED"));
        assertThrows(CustomException.class, () -> validateRecurrence(RecurrenceFreq.MONTHLY, "0WED"));
        assertThrows(CustomException.class, () -> validateRecurrence(RecurrenceFreq.MONTHLY, "6WED"));
    }
}
