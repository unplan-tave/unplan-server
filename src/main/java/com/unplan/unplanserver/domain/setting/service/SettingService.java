package com.unplan.unplanserver.domain.setting.service;

import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingRequestDto;
import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingResponseDto;
import com.unplan.unplanserver.domain.setting.entity.RecommendBanTime;
import com.unplan.unplanserver.domain.setting.entity.Setting;
import com.unplan.unplanserver.domain.setting.repository.RecommendBanTimeRepository;
import com.unplan.unplanserver.domain.setting.repository.SettingRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettingService {
    private final SettingRepository settingRepository;
    @Transactional
    public EmptyTimeSettingResponseDto updateSetting(Long memberId, EmptyTimeSettingRequestDto requestDto) {
        Setting setting = settingRepository.findByMemberId(memberId).orElse(null);
        if (setting == null) {
            setting = new Setting(memberId);
            settingRepository.save(setting);
        }
        if (requestDto.recommendBanTimes() != null) {
            validate(requestDto.recommendBanTimes());
        }

        setting.update(requestDto);
        return setting.toResponseDto();
    }

    @Transactional
    public EmptyTimeSettingResponseDto getSetting(Long memberId) {
        try {
            return settingRepository.findByMemberId(memberId)
                    .orElseGet(() -> settingRepository.save(new Setting(memberId)))
                    .toResponseDto();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return settingRepository.findByMemberId(memberId)
                    .orElseThrow(() -> new CustomException(ErrorCode.SETTING_NOT_FOUND)).toResponseDto();
        }
    }
    public void validate(List<EmptyTimeSettingRequestDto.RecommendBanTime> requestedBanTimeList) {
        requestedBanTimeList.sort(Comparator.comparing(EmptyTimeSettingRequestDto.RecommendBanTime::startTime));
        for (EmptyTimeSettingRequestDto.RecommendBanTime recommendBanTime : requestedBanTimeList) {
            LocalTime startTime = recommendBanTime.startTime();
            LocalTime endTime = recommendBanTime.endTime();
            if (startTime == null || endTime == null) {
                throw new CustomException(ErrorCode.INVALID_REQUEST);
            }
            if (endTime.isBefore(startTime)) {
                throw new CustomException(ErrorCode.INVALID_TIME_RANGE);
            }
        }
        for (int i = 0; i < requestedBanTimeList.size() - 1; i++) {
            if (requestedBanTimeList.get(i).endTime().isAfter(requestedBanTimeList.get(i + 1).startTime())) {
                throw new CustomException(ErrorCode.TIME_RANGE_OVERLAP);
            }
        }
    }


}
