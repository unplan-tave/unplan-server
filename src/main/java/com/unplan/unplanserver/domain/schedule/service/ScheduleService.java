package com.unplan.unplanserver.domain.schedule.service;

import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleCreateResponse;
import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Transactional
    public ScheduleCreateResponse createSchedule(Long memberId, ScheduleCreateRequest request) {

        // 1. Schedule 엔티티 생성
        // Request DTO에서 값을 꺼내서 Schedule entity를 만듦
        Schedule schedule = Schedule.builder()
                .memberId(memberId)
                .title(request.getTitle())
                .conditionTag(request.getConditionTag())
                .date(request.getDate() != null ? LocalDate.parse(request.getDate()) : null)
                .startTime(request.getStartTime() != null ? LocalTime.parse(request.getStartTime()) : null)
                .endTime(request.getEndTime() != null ? LocalTime.parse(request.getEndTime()) : null)
                .estimatedTime(request.getEstimatedTime())
                .memo(request.getMemo())
                .isRemindOn(request.getIsRemindOn())
                .remindMinutes(request.getRemindMinutes())
                .remindType(request.getRemindType())
                .remindSoundType(request.getRemindSoundType())
                .isQueue(request.getStartTime() == null && request.getEndTime() == null)
                .isRecurring(request.getRecurrence() != null)
                .isConflict(false)
                .status(ScheduleStatus.TODO)
                .build();

        Schedule saved = scheduleRepository.save(schedule);

        // 2. Response 반환
        return ScheduleCreateResponse.builder()
                .scheduleId(saved.getScheduleId())
                .title(saved.getTitle())
                .date(saved.getDate() != null ? saved.getDate().toString() : null)
                .startTime(saved.getStartTime() != null ? saved.getStartTime().toString() : null)
                .endTime(saved.getEndTime() != null ? saved.getEndTime().toString() : null)
                .estimatedTime(saved.getEstimatedTime())
                .isQueue(saved.getIsQueue())
                .build();
    }
}