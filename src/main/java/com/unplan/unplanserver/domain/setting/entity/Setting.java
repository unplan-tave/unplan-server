package com.unplan.unplanserver.domain.setting.entity;

import com.unplan.unplanserver.domain.setting.dto.AlarmSettingRequestDto;
import com.unplan.unplanserver.domain.setting.dto.AlarmSettingResponseDto;
import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingRequestDto;
import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingResponseDto;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.*;

@Entity
@Getter
@Slf4j
@RequiredArgsConstructor
public class Setting {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "setting_id")
    private Long settingId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "is_schedule_end_alarm_on", nullable = false)
    private Boolean isScheduleEndAlarmOn;

    @Column(name = "is_condition_record_alarm_on", nullable = false)
    private Boolean isConditionRecordAlarmOn;

    @Column(name = "is_recommend_alarm_on", nullable = false)
    private Boolean isRecommendAlarmOn;

    @Column(name = "is_empty_time_recommend_on", nullable = false)
    private Boolean isEmptyTimeRecommendOn;

    @Column(name = "empty_time_criteria_minutes", nullable = false)
    private Integer emptyTimeCriteriaMinutes;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "setting_id")
    private List<RecommendBanTime> recommendBanTimeList;

    @Column(name = "is_recommend_ban_time_on", nullable = false)
    private Boolean isRecommendBanTimeOn;

    public Setting(Long memberId) {
        this.memberId = memberId;
        this.isScheduleEndAlarmOn = true;
        this.isConditionRecordAlarmOn = true;
        this.isRecommendAlarmOn = true;
        this.isEmptyTimeRecommendOn = true;
        this.emptyTimeCriteriaMinutes = 15;
        this.recommendBanTimeList = new ArrayList<>();
        this.recommendBanTimeList.add(new RecommendBanTime(this.settingId, LocalTime.of(0, 0), LocalTime.of(7, 0)));
        this.recommendBanTimeList.add(new RecommendBanTime(this.settingId, LocalTime.of(22, 0), LocalTime.of(23, 59)));
        this.isRecommendBanTimeOn = true;
    }

    public void update(EmptyTimeSettingRequestDto requestDto) {
        if (requestDto.isEmptyTimeRecommendOn() != null) {
            this.isEmptyTimeRecommendOn = requestDto.isEmptyTimeRecommendOn();
        }
        if (requestDto.emptyTimeCriteriaMinutes() != null) {
            this.emptyTimeCriteriaMinutes = requestDto.emptyTimeCriteriaMinutes();
        }
        if (requestDto.isRecommendBanTimeOn() != null) {
            this.isRecommendBanTimeOn = requestDto.isRecommendBanTimeOn();
        }
        if (requestDto.recommendBanTimes() != null) {
            this.recommendBanTimeList.clear();
            for (EmptyTimeSettingRequestDto.RecommendBanTime requestedRecommendBanTime : requestDto.recommendBanTimes()) {
                this.recommendBanTimeList.add(new RecommendBanTime(this.settingId, requestedRecommendBanTime.startTime(), requestedRecommendBanTime.endTime()));
            }
        }
    }
    public EmptyTimeSettingResponseDto toResponseDto() {
        List<EmptyTimeSettingResponseDto.RecommendBanTime> recommendBanTimesResponse = new ArrayList<>();
        for (RecommendBanTime recommendBanTime : this.recommendBanTimeList) {
            recommendBanTimesResponse.add(new EmptyTimeSettingResponseDto.RecommendBanTime(recommendBanTime.getStartTime(), recommendBanTime.getEndTime()));
        }

        return new EmptyTimeSettingResponseDto(this.isEmptyTimeRecommendOn, this.emptyTimeCriteriaMinutes, this.isRecommendBanTimeOn, recommendBanTimesResponse);
    }

    public AlarmSettingResponseDto toAlarmSettingResponseDto() {
        return new AlarmSettingResponseDto(this.isScheduleEndAlarmOn, this.isConditionRecordAlarmOn, this.isRecommendAlarmOn);
    }

    public void updateAlarmSetting(AlarmSettingRequestDto requestDto) {
        if (requestDto == null) {
            return;
        }
        Boolean isScheduleEndAlarmOn = requestDto.isScheduleEndAlarmOn();
        Boolean isConditionRecordAlarmOn = requestDto.isConditionRecordAlarmOn();
        Boolean isRecommendAlarmOn = requestDto.isRecommendAlarmOn();

        if (isScheduleEndAlarmOn != null) {
            this.isScheduleEndAlarmOn = isScheduleEndAlarmOn;
        }
        if (isConditionRecordAlarmOn != null) {
            this.isConditionRecordAlarmOn = isConditionRecordAlarmOn;
        }
        if (isRecommendAlarmOn != null) {
            this.isRecommendAlarmOn = isRecommendAlarmOn;
        }

    }
}
