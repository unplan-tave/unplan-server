package com.unplan.unplanserver.domain.schedule.entity;

import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "recurrence_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecurrenceRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recurrence_id")
    private Long recurrenceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "freq")
    private RecurrenceFreq freq;

    @Column(name = "interval")
    private Integer interval;

    // "MON,WED" 형태의 문자열로 저장
    @Column(name = "by_day")
    private String byDay;

    // "1,17" 형태의 문자열로 저장
    @Column(name = "by_month_day")
    private String byMonthDay;

    @Column(name = "until")
    private LocalDate until;
}