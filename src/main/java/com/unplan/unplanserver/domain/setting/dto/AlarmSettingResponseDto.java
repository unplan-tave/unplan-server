package com.unplan.unplanserver.domain.setting.dto;

public record AlarmSettingResponseDto (
        Boolean isScheduleEndAlarmOn,
        Boolean isConditionRecordAlarmOn,
        Boolean isRecommendAlarmOn
){
}
