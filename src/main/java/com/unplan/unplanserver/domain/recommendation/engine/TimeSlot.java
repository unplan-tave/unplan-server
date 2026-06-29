package com.unplan.unplanserver.domain.recommendation.engine;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 추천 가능한 빈 시간 슬롯 (특정 날짜의 [start, end)).
 * EmptyTimeFinder 가 산출하며, 이후 큐 카드 소요시간/컨디션 매칭의 후보가 된다.
 */
public record TimeSlot(LocalDate date, LocalTime start, LocalTime end) {

    public int durationMinutes() {
        return (end.getHour() * 60 + end.getMinute()) - (start.getHour() * 60 + start.getMinute());
    }
}
