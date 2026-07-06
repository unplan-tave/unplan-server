package com.unplan.unplanserver.domain.setting.service;

import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingRequestDto;
import com.unplan.unplanserver.domain.setting.dto.EmptyTimeSettingResponseDto;
import com.unplan.unplanserver.domain.setting.entity.RecommendBanTime;
import com.unplan.unplanserver.domain.setting.entity.Setting;
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
            validate(setting.getSettingId(), requestDto.recommendBanTimes(), setting.getRecommendBanTimeList());
        }

        return setting.toResponseDto(requestDto);
    }

    public void validate(Long settingId, List<EmptyTimeSettingRequestDto.RecommendBanTime> recommendBanTimes, List<RecommendBanTime> existing) {
        List<RecommendBanTime> all = new ArrayList<>();
        all.addAll(existing);
        //request dto 내부 조건 검사
        for (EmptyTimeSettingRequestDto.RecommendBanTime recommendBanTime : recommendBanTimes) {
            LocalTime startTime = recommendBanTime.startTime();
            LocalTime endTime = recommendBanTime.endTime();
            if (endTime.isBefore(startTime)) {
                throw new CustomException(ErrorCode.INVALID_TIME_RANGE);
            }
            all.add(new RecommendBanTime(settingId, startTime, endTime));
        }
        // 기존 시간대와 맞는지
        all.sort(Comparator.comparing(RecommendBanTime::getStartTime));
        for (int i = 0; i < all.size() - 1; i++) {
            if (all.get(i).getEndTime().isAfter(all.get(i + 1).getStartTime())) {
                throw new CustomException(ErrorCode.TIME_RANGE_OVERLAP);
            }
        }
    }
}
