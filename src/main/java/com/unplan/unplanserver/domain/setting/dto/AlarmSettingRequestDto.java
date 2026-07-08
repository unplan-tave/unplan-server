package com.unplan.unplanserver.domain.setting.dto;

public record AlarmSettingRequestDto(
        Boolean isScheduleEndAlarmOn,
        Boolean isConditionRecordAlarmOn,
        Boolean isRecommendAlarmOn
) {
}
