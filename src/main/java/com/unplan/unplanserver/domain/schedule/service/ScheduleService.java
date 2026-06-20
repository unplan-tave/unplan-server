package com.unplan.unplanserver.domain.schedule.service;

import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleCreateResponse;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleDetailResponse;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleGetResponse;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleWeeklyResponse;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleMonthlyResponse;
import com.unplan.unplanserver.domain.schedule.entity.LocationInfo;
import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.LocationInfoRepository;
import com.unplan.unplanserver.domain.schedule.repository.RecurrenceRuleRepository;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final LocationInfoRepository locationInfoRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;

    @Transactional
    public ScheduleCreateResponse createSchedule(Long memberId, ScheduleCreateRequest request) {

        // 1. Schedule 엔티티 생성
        // Request DTO에서 값을 꺼내서 Schedule entity를 만듦
        Schedule schedule = Schedule.builder()
                .memberId(memberId)
                .title(request.getTitle())
                .conditionTag(request.getConditionTag())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
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

        // 2. 위치 정보 저장
        if (request.getLatitude() != null && request.getLongitude() != null) {
            locationInfoRepository.save(LocationInfo.builder()
                    .schedule(saved)
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .build());
        }

        // 3. 반복 설정 저장
        if (request.getRecurrence() != null) {
            ScheduleCreateRequest.RecurrenceRequest rec = request.getRecurrence();
            recurrenceRuleRepository.save(RecurrenceRule.builder()
                    .schedule(saved)
                    .freq(rec.getFreq())
                    .interval(rec.getInterval())
                    .byDay(rec.getByDay())
                    .byMonthDay(rec.getByMonthDay())
                    .until(rec.getUntil())
                    .count(rec.getCount())
                    .build());
        }

        // 4. Response 반환
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

    @Transactional(readOnly = true)
    public List<ScheduleGetResponse> getSchedulesByDate(Long memberId, LocalDate date) {
        return scheduleRepository.findByMemberIdAndDate(memberId, date)
                .stream()
                .map(ScheduleGetResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ScheduleDetailResponse getScheduleDetail(Long memberId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findByScheduleIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        LocationInfo locationInfo = locationInfoRepository.findBySchedule(schedule).orElse(null);
        return ScheduleDetailResponse.from(schedule, locationInfo);
    }

    @Transactional
    public ScheduleDetailResponse updateSchedule(Long memberId, Long scheduleId, ScheduleUpdateRequest request) {
        Schedule schedule = scheduleRepository.findByScheduleIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        schedule.update(request);
        LocationInfo locationInfo = locationInfoRepository.findBySchedule(schedule).orElse(null);
        return ScheduleDetailResponse.from(schedule, locationInfo);
    }

    @Transactional
    public void deleteSchedule(Long memberId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findByScheduleIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        scheduleRepository.delete(schedule);
    }

    @Transactional(readOnly = true)
    public ScheduleWeeklyResponse getSchedulesByWeek(Long memberId, LocalDate date) {
        LocalDate weekStart = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        List<Schedule> schedules = scheduleRepository.findByMemberIdAndDateBetween(memberId, weekStart, weekEnd);

        Map<LocalDate, List<Schedule>> byDate = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getDate));

        List<ScheduleWeeklyResponse.DailySchedules> weeklySchedules = Stream.iterate(weekStart, d -> d.plusDays(1))
                .limit(7)
                .map(d -> ScheduleWeeklyResponse.DailySchedules.builder()
                        .date(d.toString())
                        .schedules(byDate.getOrDefault(d, List.of()).stream()
                                .map(s -> ScheduleWeeklyResponse.ScheduleSummary.builder()
                                        .scheduleId(s.getScheduleId())
                                        .title(s.getTitle())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return ScheduleWeeklyResponse.builder()
                .weeklySchedules(weeklySchedules)
                .build();
    }

    @Transactional(readOnly = true)
    public ScheduleMonthlyResponse getSchedulesByMonth(Long memberId, YearMonth yearMonth) {
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay = yearMonth.atEndOfMonth();

        LocalDate viewStart = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate viewEnd = lastDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        List<Schedule> schedules = scheduleRepository.findByMemberIdAndDateBetween(memberId, viewStart, viewEnd);

        List<ScheduleMonthlyResponse.DailyCount> dailyCounts = schedules.stream()
                .collect(Collectors.groupingBy(Schedule::getDate, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> ScheduleMonthlyResponse.DailyCount.builder()
                        .date(e.getKey().toString())
                        .count(e.getValue().intValue())
                        .build())
                .toList();

        return ScheduleMonthlyResponse.builder()
                .yearMonth(yearMonth.toString())
                .schedules(dailyCounts)
                .build();
    }
}