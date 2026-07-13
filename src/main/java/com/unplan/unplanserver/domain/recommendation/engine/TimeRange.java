package com.unplan.unplanserver.domain.recommendation.engine;

import java.time.LocalTime;

/**
 * 하루 안에서의 시각 구간 [start, end). 핀 카드가 점유한 시간(busy) 표현에 사용한다.
 * 날짜 정보는 갖지 않으며, 빈 시간 탐색은 하루(LocalDate) 단위로 수행한다.
 */
public record TimeRange(LocalTime start, LocalTime end) {

    public int durationMinutes() {
        return toMinuteOfDay(end) - toMinuteOfDay(start);
    }

    static int toMinuteOfDay(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }
}
