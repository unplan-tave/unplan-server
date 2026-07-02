package com.unplan.unplanserver.domain.recommendation.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 빈 시간 탐색 엔진(EmptyTimeFinder)의 순수 시간 계산 검증. DB/스프링 없이 동작.
 */
class EmptyTimeFinderTest {

    private final EmptyTimeFinder finder = new EmptyTimeFinder();
    private final LocalDate date = LocalDate.parse("2026-06-30");

    private LocalTime t(String s) { return LocalTime.parse(s); }
    private TimeRange busy(String s, String e) { return new TimeRange(t(s), t(e)); }
    private TimeSlot slot(String s, String e) { return new TimeSlot(date, t(s), t(e)); }

    private List<TimeSlot> find(LocalTime ws, LocalTime we, List<TimeRange> busy, int buffer, int minGap) {
        return finder.findFreeSlots(date, ws, we, busy, buffer, minGap);
    }

    @Test
    @DisplayName("핀 카드가 없으면 탐색 범위 전체가 하나의 빈 슬롯")
    void noBusyReturnsWholeWindow() {
        assertEquals(List.of(slot("09:00", "18:00")),
                find(t("09:00"), t("18:00"), List.of(), 15, 0));
    }

    @Test
    @DisplayName("핀 카드 앞뒤 버퍼(15분)를 제외하고 빈 슬롯을 나눔")
    void singlePinWithBuffer() {
        // 핀 14:00~15:00, buffer 15 → 차단 13:45~15:15
        List<TimeSlot> slots = find(t("09:00"), t("18:00"), List.of(busy("14:00", "15:00")), 15, 0);
        assertEquals(List.of(slot("09:00", "13:45"), slot("15:15", "18:00")), slots);
    }

    @Test
    @DisplayName("겹치거나 버퍼로 인접해지는 핀들은 하나의 차단 구간으로 병합")
    void overlappingPinsMerged() {
        // 핀 10:00~11:00, 11:10~12:00 / buffer 15 → 09:45~11:15, 10:55~12:15 겹침 → 09:45~12:15
        List<TimeSlot> slots = find(t("09:00"), t("13:00"),
                List.of(busy("10:00", "11:00"), busy("11:10", "12:00")), 15, 0);
        assertEquals(List.of(slot("09:00", "09:45"), slot("12:15", "13:00")), slots);
    }

    @Test
    @DisplayName("minGap(최소 여유 시간) 미만 길이의 슬롯은 버림")
    void minGapFiltersShortSlots() {
        // window 09:00~10:00, 핀 09:15~09:20 / 09:35~09:40 (buffer 0) → 사이 15분 슬롯들은 20분 미만이라 제외
        List<TimeSlot> slots = find(t("09:00"), t("10:00"),
                List.of(busy("09:15", "09:20"), busy("09:35", "09:40")), 0, 20);
        // 09:00~09:15(15분) 제외, 09:20~09:35(15분) 제외, 09:40~10:00(20분) 채택
        assertEquals(List.of(slot("09:40", "10:00")), slots);
    }

    @Test
    @DisplayName("탐색 범위 밖의 핀은 무시")
    void pinOutsideWindowIgnored() {
        // 핀 06:00~07:00 (+buffer 15 = 05:45~07:15) 은 window 09:00~ 밖 → 전체가 빈 시간
        List<TimeSlot> slots = find(t("09:00"), t("18:00"), List.of(busy("06:00", "07:00")), 15, 0);
        assertEquals(List.of(slot("09:00", "18:00")), slots);
    }

    @Test
    @DisplayName("버퍼가 탐색 범위 시작을 넘어가면 범위 경계로 클리핑")
    void bufferClippedAtWindowEdge() {
        // 핀 09:05~10:00 (+buffer 15 = 08:50~10:15) → 시작은 09:00로 클리핑 → 앞 슬롯 없음, 10:15~18:00만
        List<TimeSlot> slots = find(t("09:00"), t("18:00"), List.of(busy("09:05", "10:00")), 15, 0);
        assertEquals(List.of(slot("10:15", "18:00")), slots);
    }

    @Test
    @DisplayName("windowStart >= windowEnd 이면 빈 목록")
    void emptyWindow() {
        assertTrue(find(t("18:00"), t("09:00"), List.of(), 15, 0).isEmpty());
    }

    @Test
    @DisplayName("filterByDuration — 소요시간 이상인 슬롯만, 미정(null)이면 빈 목록")
    void filterByDuration() {
        List<TimeSlot> slots = List.of(slot("09:00", "09:40"), slot("10:00", "12:00"));
        assertEquals(List.of(slot("10:00", "12:00")), finder.filterByDuration(slots, 60)); // 40분 슬롯 탈락
        assertEquals(slots, finder.filterByDuration(slots, 30));
        assertTrue(finder.filterByDuration(slots, null).isEmpty());
    }

    // ─────────────────────────── 자정(24:00)·초 단위 경계 규약 ───────────────────────────

    @Test
    @DisplayName("windowEnd=00:00 은 '그날의 끝(24:00)' — 마지막 1분이 유실되지 않음")
    void midnightWindowEndMeansEndOfDay() {
        // 23:00~24:00 슬롯(60분)에 60분짜리 카드가 들어가야 함 (23:59로 자르면 59분이 되어 탈락)
        List<TimeSlot> slots = find(t("23:00"), LocalTime.MIDNIGHT, List.of(), 15, 0);
        assertEquals(List.of(slot("23:00", "00:00")), slots);
        assertEquals(60, slots.get(0).durationMinutes());
        assertEquals(slots, finder.filterByDuration(slots, 60));
    }

    @Test
    @DisplayName("windowEnd=00:00 + 핀 카드 — 버퍼 이후부터 자정까지가 빈 슬롯")
    void midnightWindowEndWithPin() {
        // 핀 22:00~23:00 (+buffer 15 → 차단 21:45~23:15) → 20:00~21:45, 23:15~24:00(45분)
        List<TimeSlot> slots = find(t("20:00"), LocalTime.MIDNIGHT, List.of(busy("22:00", "23:00")), 15, 0);
        assertEquals(List.of(slot("20:00", "21:45"), slot("23:15", "00:00")), slots);
        assertEquals(45, slots.get(1).durationMinutes());
    }

    @Test
    @DisplayName("windowStart 에 초가 있으면 다음 분으로 올림 — 슬롯이 과거 시각에서 시작하지 않음")
    void windowStartSecondsRoundedUp() {
        // '현재 시각' 14:30:45 → 슬롯은 14:31부터 (내림하면 14:30 = 현재보다 과거)
        List<TimeSlot> slots = find(LocalTime.of(14, 30, 45), t("16:00"), List.of(), 15, 0);
        assertEquals(List.of(slot("14:31", "16:00")), slots);
    }
}
