package com.unplan.unplanserver.domain.setting.entity;

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
        this.emptyTimeCriteriaMinutes = 10;
        this.recommendBanTimeList = new ArrayList<>();
        this.isRecommendBanTimeOn = true;
    }
    public EmptyTimeSettingResponseDto toResponseDto(EmptyTimeSettingRequestDto requestDto) {
        Boolean isEmptyTimeRecommendOn = requestDto.isEmptyTimeRecommendOn();
        Integer emptyTimeCriteriaMinutes = requestDto.emptyTimeCriteriaMinutes();
        Boolean isRecommendBanTimeOn = requestDto.isRecommendBanTimeOn();
        List<EmptyTimeSettingRequestDto.RecommendBanTime> recommendBanTimes = requestDto.recommendBanTimes();
        if (isEmptyTimeRecommendOn != null) {
            this.isEmptyTimeRecommendOn = isEmptyTimeRecommendOn;
        }
        if (emptyTimeCriteriaMinutes != null) {
            this.emptyTimeCriteriaMinutes = emptyTimeCriteriaMinutes;
        }
        if (isRecommendBanTimeOn != null) {
            this.isRecommendBanTimeOn = isRecommendBanTimeOn;
        }
        List<EmptyTimeSettingResponseDto.RecommendBanTime> recommendBanTimesResponse = new ArrayList<>();
        // response dto에 기존 추천금지 시간대 목록 추가
        log.info(this.recommendBanTimeList.toString());
        for (RecommendBanTime recommendBanTime : this.recommendBanTimeList) {
            LocalTime startTime = recommendBanTime.getStartTime();
            LocalTime endTime = recommendBanTime.getEndTime();
            EmptyTimeSettingResponseDto.RecommendBanTime responseRecommendBanTime = new EmptyTimeSettingResponseDto.RecommendBanTime(startTime, endTime);
            recommendBanTimesResponse.add(responseRecommendBanTime);
        }
        if (recommendBanTimes != null) {
            // response dto에 입력받은 추천금지 시간대 추가
            for (EmptyTimeSettingRequestDto.RecommendBanTime requestedRecommendBanTime : recommendBanTimes) {
                LocalTime startTime = requestedRecommendBanTime.startTime();
                LocalTime endTime = requestedRecommendBanTime.endTime();
                RecommendBanTime recommendBanTime = new RecommendBanTime(this.settingId, startTime, endTime);
                this.recommendBanTimeList.add(recommendBanTime);
                recommendBanTimesResponse.add(new EmptyTimeSettingResponseDto.RecommendBanTime(startTime, endTime));
            }
        }

        return new EmptyTimeSettingResponseDto(this.isEmptyTimeRecommendOn, this.emptyTimeCriteriaMinutes, this.isRecommendBanTimeOn, recommendBanTimesResponse);
    }
}
