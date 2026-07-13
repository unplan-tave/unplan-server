package com.unplan.unplanserver.domain.recommendation.engine;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 추천 가능한 빈 시간 슬롯 (특정 날짜의 [start, end)).
 * EmptyTimeFinder 가 산출하며, 이후 큐 카드 소요시간/컨디션 매칭의 후보가 된다.
 * end 가 00:00 이면 '그날의 끝(24:00)'을 뜻한다 (EmptyTimeFinder 경계 규약).
 */
public record TimeSlot(LocalDate date, LocalTime start, LocalTime end) {

    public int durationMinutes() {
        int endMin = end.equals(LocalTime.MIDNIGHT)
                ? EmptyTimeFinder.END_OF_DAY_MIN
                : end.getHour() * 60 + end.getMinute();
        return endMin - (start.getHour() * 60 + start.getMinute());
    }
}
