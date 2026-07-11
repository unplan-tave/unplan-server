package com.unplan.unplanserver.domain.setting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;
@Entity
@RequiredArgsConstructor
@Getter
@Table(name = "recommend_ban_time")
public class RecommendBanTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommend_ban_time_id")
    private Long recommendBanTimeId;

    @Column(name = "setting_id")
    private Long settingId;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    public RecommendBanTime(Long settingId, LocalTime startTime, LocalTime endTime) {
        this.settingId = settingId;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
