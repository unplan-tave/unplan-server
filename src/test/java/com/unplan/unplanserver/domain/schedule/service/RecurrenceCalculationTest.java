package com.unplan.unplanserver.domain.schedule.service;

import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 반복 일정 동적 계산(calculateInstancesInRange)의 순수 날짜 로직 검증.
 * DB/스프링 컨텍스트 없이 리플렉션으로 private 메서드를 직접 호출한다.
 * 특히 경계값에서 예외가 터지지 않는지 집중 검증.
 */
class RecurrenceCalculationTest {

    // 날짜 계산만 검증하므로 모든 의존성(TagService 포함)은 null 로 둔다.
    private final ScheduleService service = new ScheduleService(null, null, null, null);

    @SuppressWarnings("unchecked")
    private List<LocalDate> calc(LocalDate original, RecurrenceRule rule, LocalDate start, LocalDate end) {
        try {
            Method m = ScheduleService.class.getDeclaredMethod(
                    "calculateInstancesInRange",
                    LocalDate.class, RecurrenceRule.class, LocalDate.class, LocalDate.class);
            m.setAccessible(true);
            return (List<LocalDate>) m.invoke(service, original, rule, start, end);
        } catch (InvocationTargetException e) {
            // 실제 발생한 예외를 그대로 노출 (assertDoesNotThrow가 감지할 수 있도록)
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private LocalDate d(String s) { return LocalDate.parse(s); }

    // ─────────────────────────── until / count 경계 (B1~B3) ───────────────────────────

    @Test
    @DisplayName("B1: count=1 이면 원본 외 인스턴스 없음")
    void dailyCount1() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.DAILY).interval(1).count(1).build();
        assertTrue(calc(d("2026-06-29"), rule, d("2026-06-30"), d("2026-06-30")).isEmpty());
    }

    @Test
    @DisplayName("B2: until=원본날짜 이면 다음 날 없음")
    void dailyUntilEqualsOriginal() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.DAILY).interval(1).until(d("2026-06-29")).build();
        assertTrue(calc(d("2026-06-29"), rule, d("2026-06-30"), d("2026-06-30")).isEmpty());
    }

    @Test
    @DisplayName("B3: until 은 inclusive — 해당 일 포함, 그 다음 날 제외")
    void dailyUntilInclusive() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.DAILY).interval(1).until(d("2026-06-30")).build();
        assertEquals(List.of(d("2026-06-30")), calc(d("2026-06-29"), rule, d("2026-06-30"), d("2026-06-30")));
        assertTrue(calc(d("2026-06-29"), rule, d("2026-07-01"), d("2026-07-01")).isEmpty());
    }

    // ─────────────────────────── MONTHLY N번째 요일 폴백 (B4 + 6주차) ───────────────────────────

    @Test
    @DisplayName("B4: 5번째 월요일 — 없는 달은 마지막 월요일로 폴백, overflow 안 함")
    void monthlyFifthMonday() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.MONTHLY).interval(1).byDay("5MON").build();
        LocalDate orig = d("2026-06-29"); // 6월 5번째 월요일
        assertEquals(List.of(d("2026-07-27")), calc(orig, rule, d("2026-07-01"), d("2026-07-31"))); // 폴백
        assertEquals(List.of(d("2026-08-31")), calc(orig, rule, d("2026-08-01"), d("2026-08-31"))); // 실제 5번째 존재
        assertTrue(calc(orig, rule, d("2026-08-03"), d("2026-08-03")).isEmpty());                   // 1번째 월요일 아님
    }

    @Test
    @DisplayName("6번째 요일 — 6주차 없는 달도 예외 없이 마지막 요일로 폴백")
    void monthlySixthMonday() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.MONTHLY).interval(1).byDay("6MON").build();
        LocalDate orig = d("2026-06-29");
        List<LocalDate> july = assertDoesNotThrow(() -> calc(orig, rule, d("2026-07-01"), d("2026-07-31")));
        assertEquals(List.of(d("2026-07-27")), july); // 7월은 6번째 월요일 없음 → 마지막 월요일
    }

    // ─────────────────────────── byMonthDay 다중값 / 클램핑 (수정한 버그) ───────────────────────────

    @Test
    @DisplayName("byMonthDay '30,31' — 말일에 겹쳐도 중복 인스턴스 생기지 않음")
    void byMonthDayClampDedup() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.MONTHLY).interval(1).byMonthDay("30,31").build();
        // 2026-02 는 28일까지 → 30, 31 둘 다 28로 클램핑 → dedup 후 1개
        List<LocalDate> feb = calc(d("2026-01-30"), rule, d("2026-02-01"), d("2026-02-28"));
        assertEquals(List.of(d("2026-02-28")), feb);
        assertEquals(1, feb.size(), "중복 제거 실패");
    }

    @Test
    @DisplayName("byMonthDay '1,17' — 원본 달의 원본 이후 날짜(17일)가 누락되지 않음")
    void byMonthDayOriginalMonthNotMissed() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.MONTHLY).interval(1).byMonthDay("1,17").build();
        List<LocalDate> june = calc(d("2026-06-01"), rule, d("2026-06-01"), d("2026-06-30"));
        assertTrue(june.contains(d("2026-06-17")), "원본 달 17일 누락");
    }

    @Test
    @DisplayName("byMonthDay '31' — 31일 없는 달(4월)은 30일로 당김, 예외 없음")
    void byMonthDay31InShortMonth() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.MONTHLY).interval(1).byMonthDay("31").build();
        List<LocalDate> apr = assertDoesNotThrow(() -> calc(d("2026-01-31"), rule, d("2026-04-01"), d("2026-04-30")));
        assertEquals(List.of(d("2026-04-30")), apr);
    }

    // ─────────────────────────── MONTHLY 다중 요일 (예외 방지) ───────────────────────────

    @Test
    @DisplayName("MONTHLY byDay 'MON,WED' — 콤마 다중 요일도 예외 없이 각각 생성")
    void monthlyMultiWeekdayNoException() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.MONTHLY).interval(1).byDay("MON,WED").build();
        LocalDate orig = d("2026-06-09"); // 6월 2번째 화요일 → nth=2
        List<LocalDate> july = assertDoesNotThrow(() -> calc(orig, rule, d("2026-07-01"), d("2026-07-31")));
        // 7월 2번째 월요일=13, 2번째 수요일=8
        assertEquals(List.of(d("2026-07-08"), d("2026-07-13")), july);
    }

    // ─────────────────────────── YEARLY 윤년 2/29 ───────────────────────────

    @Test
    @DisplayName("YEARLY 2/29 — 평년은 2/28, 윤년(2028)엔 다시 2/29 (드리프트 없음)")
    void yearlyLeapDay() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.YEARLY).interval(1).build();
        LocalDate orig = d("2024-02-29");
        assertEquals(List.of(d("2026-02-28")), calc(orig, rule, d("2026-02-01"), d("2026-02-28"))); // 평년
        assertEquals(List.of(d("2028-02-29")), calc(orig, rule, d("2028-02-01"), d("2028-02-29"))); // 윤년 복원
    }

    // ─────────────────────────── 파서 하드닝 (예외 방지) ───────────────────────────

    @Test
    @DisplayName("byMonthDay 끝 콤마/범위 밖 값 — 예외 없이 무시")
    void byMonthDayMalformedNoException() {
        RecurrenceRule trailingComma = RecurrenceRule.builder()
                .freq(RecurrenceFreq.MONTHLY).interval(1).byMonthDay("1,17,").build();
        assertDoesNotThrow(() -> calc(d("2026-06-01"), trailingComma, d("2026-07-01"), d("2026-07-31")));

        RecurrenceRule outOfRange = RecurrenceRule.builder()
                .freq(RecurrenceFreq.MONTHLY).interval(1).byMonthDay("0,31").build();
        List<LocalDate> feb = assertDoesNotThrow(() -> calc(d("2026-01-31"), outOfRange, d("2026-02-01"), d("2026-02-28")));
        assertEquals(List.of(d("2026-02-28")), feb); // 0은 제거, 31→28
    }

    @Test
    @DisplayName("byDay 끝/중복 콤마 — 예외 없이 무시")
    void byDayMalformedNoException() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.WEEKLY).interval(1).byDay("MON,,FRI").build();
        assertDoesNotThrow(() -> calc(d("2026-06-17"), rule, d("2026-06-15"), d("2026-07-31")));
    }

    // ─────────────────────────── WEEKLY 다중 요일 + count ───────────────────────────

    @Test
    @DisplayName("WEEKLY 'MON,FRI' count=5 — 원본 포함 총 5개(추가 4개)로 제한")
    void weeklyMultiDayCount() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.WEEKLY).interval(1).byDay("MON,FRI").count(5).build();
        LocalDate orig = d("2026-06-17"); // 수요일
        List<LocalDate> got = calc(orig, rule, d("2026-06-15"), d("2026-07-31"));
        // 원본(#1) 이후: 06-19(금,#2), 06-22(월,#3), 06-26(금,#4), 06-29(월,#5)
        assertEquals(List.of(d("2026-06-19"), d("2026-06-22"), d("2026-06-26"), d("2026-06-29")), got);
    }

    @Test
    @DisplayName("WEEKLY interval=2 byDay 'SUN' — 일요일 기준 주기로 격주 일요일 생성 (월요일 기준이면 결과 달라짐)")
    void weeklyBiweeklySundayAnchored() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.WEEKLY).interval(2).byDay("SUN").build();
        LocalDate orig = d("2026-06-17"); // 수요일, 같은 주 일요일(06-14)은 원본 이전 → 다음 주기부터
        List<LocalDate> got = calc(orig, rule, d("2026-06-15"), d("2026-07-31"));
        // 일요일 앵커(06-14) 기준 격주: 06-28, 07-12, 07-26 (월요일 앵커면 06-21, 07-05, 07-19)
        assertEquals(List.of(d("2026-06-28"), d("2026-07-12"), d("2026-07-26")), got);
    }

    @Test
    @DisplayName("WEEKLY 다중 요일 'SUN,WED' — 반환 목록이 항상 연대순으로 정렬됨")
    void weeklyMultiDayChronologicalOrder() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.WEEKLY).interval(1).byDay("SUN,WED").build();
        LocalDate orig = d("2026-06-17"); // 수요일
        List<LocalDate> got = calc(orig, rule, d("2026-06-15"), d("2026-06-30"));
        assertEquals(List.of(d("2026-06-21"), d("2026-06-24"), d("2026-06-28")), got);
    }

    @Test
    @DisplayName("WEEKLY 'SUN,WED' + count — 절단이 연대순으로 적용됨 (일요일이 잘리고 수요일이 남으면 안 됨)")
    void weeklyCountCutsChronologically() {
        // 원본 2026-07-01(수). 연대순 다음 인스턴스는 07-05(일) → count=2 면 07-05 하나만 추가되어야 한다.
        // 요일을 ISO 순서(월…일)로 돌면 07-08(수)이 먼저 생성되어 07-05가 잘리는 버그가 있었음.
        RecurrenceRule count2 = RecurrenceRule.builder()
                .freq(RecurrenceFreq.WEEKLY).interval(1).byDay("SUN,WED").count(2).build();
        assertEquals(List.of(d("2026-07-05")),
                calc(d("2026-07-01"), count2, d("2026-07-01"), d("2026-07-31")));

        RecurrenceRule count3 = RecurrenceRule.builder()
                .freq(RecurrenceFreq.WEEKLY).interval(1).byDay("SUN,WED").count(3).build();
        assertEquals(List.of(d("2026-07-05"), d("2026-07-08")),
                calc(d("2026-07-01"), count3, d("2026-07-01"), d("2026-07-31")));
    }

    // ─────────────────────────── interval 0/음수 무한 루프 방어 (코드리뷰 critical) ───────────────────────────

    @Test
    @DisplayName("interval=0 — 무한 루프 없이 1로 클램핑되어 정상 종료")
    void zeroIntervalNoInfiniteLoop() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.DAILY).interval(0).build(); // count/until 없음 → 클램핑 안 하면 무한 루프
        List<LocalDate> got = assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                () -> calc(d("2026-06-29"), rule, d("2026-06-30"), d("2026-07-02")));
        assertEquals(List.of(d("2026-06-30"), d("2026-07-01"), d("2026-07-02")), got); // interval 1 처럼 동작
    }

    @Test
    @DisplayName("interval=-3 — 음수도 1로 클램핑되어 정상 종료")
    void negativeIntervalNoInfiniteLoop() {
        RecurrenceRule rule = RecurrenceRule.builder()
                .freq(RecurrenceFreq.YEARLY).interval(-3).build();
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),
                () -> calc(d("2024-06-29"), rule, d("2026-06-29"), d("2026-06-29")));
    }
}
