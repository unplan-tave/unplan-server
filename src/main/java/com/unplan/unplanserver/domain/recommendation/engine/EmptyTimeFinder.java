package com.unplan.unplanserver.domain.recommendation.engine;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 핀 카드(고정 일정)가 점유한 시간을 제외하고, 하루 안에서 추천 가능한 빈 시간 슬롯을 계산한다.
 * 의존성이 없는 순수 로직이라 DB/스프링 없이 단위 테스트가 가능하다.
 *
 * 규칙 (Notion "추천 로직" 기준):
 *  - 핀 카드 앞뒤로 buffer(기본 15분)를 제외한 나머지를 실제 빈 시간으로 본다.
 *    (예: 핀 14:00~15:00, buffer 15 → 13:45 이전 / 15:15 이후만 빈 시간)
 *  - '최소 여유 시간'(minGap) 미만 길이의 슬롯은 버린다.
 *  - 탐색 범위 [windowStart, windowEnd) 밖의 핀/시간은 고려하지 않는다.
 *
 * 경계 규약:
 *  - windowEnd 에 00:00(MIDNIGHT)을 주면 '그날의 끝(24:00)'으로 해석한다.
 *    LocalTime 은 24:00 을 표현할 수 없어, LocalTime.MAX(23:59)를 쓰면 마지막 1분이 유실되어
 *    23:00~24:00 슬롯에 60분짜리 카드가 들어가지 못하는 경계 오류가 생기기 때문.
 *    이때 산출되는 슬롯의 end 도 00:00 이며 같은 의미다 (TimeSlot.durationMinutes 가 처리).
 *  - windowStart 에 초/나노가 있으면(예: '현재 시각') 다음 분으로 올림한다.
 *    내림하면 슬롯이 현재보다 과거 시각에서 시작해 "현재 시각 이후" 스펙에 어긋난다.
 */
@Component
public class EmptyTimeFinder {

    /** 하루의 끝(24:00)의 분 표현. windowEnd=00:00 규약이 이 값으로 환산된다. */
    static final int END_OF_DAY_MIN = 24 * 60;

    /**
     * @param date          탐색 대상 날짜 (결과 슬롯에 부여)
     * @param windowStart   탐색 시작 시각 (오늘이면 '현재 시각', 미래 날짜면 하루 시작 등). 초 이하는 올림
     * @param windowEnd     탐색 종료 시각 (windowStart 보다 뒤여야 함). 00:00 은 '그날의 끝(24:00)'
     * @param busy          핀 카드가 점유한 시간 구간들
     * @param bufferMinutes 핀 카드 앞뒤 버퍼(분). 보통 15
     * @param minGapMinutes 슬롯으로 인정할 최소 길이(분). '최소 여유 시간' 설정값, 없으면 0
     */
    public List<TimeSlot> findFreeSlots(LocalDate date, LocalTime windowStart, LocalTime windowEnd,
                                        List<TimeRange> busy, int bufferMinutes, int minGapMinutes) {
        int windowStartMin = TimeRange.toMinuteOfDay(windowStart)
                + ((windowStart.getSecond() > 0 || windowStart.getNano() > 0) ? 1 : 0);
        int windowEndMin = windowEnd.equals(LocalTime.MIDNIGHT)
                ? END_OF_DAY_MIN : TimeRange.toMinuteOfDay(windowEnd);
        if (windowStartMin >= windowEndMin) return List.of();

        // 1. 핀 구간에 버퍼를 적용하고 탐색 범위로 클리핑한 '차단 구간' 목록
        List<int[]> blocks = new ArrayList<>();
        for (TimeRange b : busy) {
            int s = TimeRange.toMinuteOfDay(b.start());
            int e = TimeRange.toMinuteOfDay(b.end());
            if (s >= e) continue; // 비정상 구간 방어
            int blockStart = Math.max(s - bufferMinutes, windowStartMin);
            int blockEnd = Math.min(e + bufferMinutes, windowEndMin);
            if (blockStart < blockEnd) blocks.add(new int[]{blockStart, blockEnd});
        }
        blocks.sort(Comparator.comparingInt(a -> a[0]));

        // 2. 겹치는 차단 구간을 병합하면서, 사이의 빈 구간을 슬롯으로 수집
        List<TimeSlot> slots = new ArrayList<>();
        int cursor = windowStartMin;
        for (int[] block : blocks) {
            if (block[0] > cursor) {
                addSlotIfLongEnough(slots, date, cursor, block[0], minGapMinutes);
            }
            cursor = Math.max(cursor, block[1]);
        }
        addSlotIfLongEnough(slots, date, cursor, windowEndMin, minGapMinutes);
        return slots;
    }

    /** 소요시간이 슬롯 길이 이내인(=들어갈 수 있는) 슬롯만 남긴다. 소요시간 미정(null)이면 빈 목록. */
    public List<TimeSlot> filterByDuration(List<TimeSlot> slots, Integer requiredMinutes) {
        if (requiredMinutes == null) return List.of();
        return slots.stream()
                .filter(s -> s.durationMinutes() >= requiredMinutes)
                .toList();
    }

    private void addSlotIfLongEnough(List<TimeSlot> slots, LocalDate date, int startMin, int endMin, int minGapMinutes) {
        int length = endMin - startMin;
        if (length > 0 && length >= minGapMinutes) {
            slots.add(new TimeSlot(date, toLocalTime(startMin), toLocalTime(endMin)));
        }
    }

    private LocalTime toLocalTime(int minuteOfDay) {
        // 24:00 은 LocalTime 으로 표현할 수 없으므로 규약대로 00:00 으로 돌려준다 (슬롯 end 전용)
        if (minuteOfDay == END_OF_DAY_MIN) return LocalTime.MIDNIGHT;
        return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60);
    }
}
