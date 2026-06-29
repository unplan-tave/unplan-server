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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

        // 3. 반복 설정 저장 (인스턴스는 조회 시 동적으로 계산)
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
        List<Schedule> schedules = new ArrayList<>(scheduleRepository.findByMemberIdAndDate(memberId, date));
        schedules.addAll(expandRecurringInstances(memberId, date, date));
        return schedules.stream()
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

        List<Schedule> schedules = new ArrayList<>(
                scheduleRepository.findByMemberIdAndDateBetween(memberId, weekStart, weekEnd));
        schedules.addAll(expandRecurringInstances(memberId, weekStart, weekEnd));

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

        List<Schedule> schedules = new ArrayList<>(
                scheduleRepository.findByMemberIdAndDateBetween(memberId, viewStart, viewEnd));
        schedules.addAll(expandRecurringInstances(memberId, viewStart, viewEnd));

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

    // ──────────────────────────────────────────────────────────────────────────
    // 반복 일정 동적 확장 로직 (조회 범위 내 인스턴스만 계산)
    // ──────────────────────────────────────────────────────────────────────────

    private List<Schedule> expandRecurringInstances(Long memberId, LocalDate rangeStart, LocalDate rangeEnd) {
        List<Schedule> originals = scheduleRepository.findByMemberIdAndIsRecurringTrue(memberId);
        if (originals.isEmpty()) return List.of();

        Map<Long, RecurrenceRule> ruleMap = recurrenceRuleRepository.findByScheduleIn(originals)
                .stream().collect(Collectors.toMap(r -> r.getSchedule().getScheduleId(), r -> r));

        List<Schedule> expanded = new ArrayList<>();
        for (Schedule original : originals) {
            if (original.getDate() == null || original.getDate().isAfter(rangeEnd)) continue;
            RecurrenceRule rule = ruleMap.get(original.getScheduleId());
            if (rule == null) continue;
            if (rule.getUntil() != null && rule.getUntil().isBefore(rangeStart)) continue;

            for (LocalDate d : calculateInstancesInRange(original.getDate(), rule, rangeStart, rangeEnd)) {
                expanded.add(Schedule.builder()
                        .scheduleId(original.getScheduleId())
                        .memberId(original.getMemberId())
                        .title(original.getTitle())
                        .conditionTag(original.getConditionTag())
                        .date(d)
                        .startTime(original.getStartTime())
                        .endTime(original.getEndTime())
                        .estimatedTime(original.getEstimatedTime())
                        .memo(original.getMemo())
                        .isRemindOn(original.getIsRemindOn())
                        .remindMinutes(original.getRemindMinutes())
                        .remindType(original.getRemindType())
                        .remindSoundType(original.getRemindSoundType())
                        .isQueue(original.getIsQueue())
                        .isRecurring(true)
                        .isConflict(false)
                        .status(original.getStatus())
                        .build());
            }
        }
        return expanded;
    }

    private List<LocalDate> calculateInstancesInRange(LocalDate originalDate, RecurrenceRule rule,
                                                      LocalDate rangeStart, LocalDate rangeEnd) {
        LocalDate searchEnd = (rule.getUntil() != null && rule.getUntil().isBefore(rangeEnd))
                ? rule.getUntil() : rangeEnd;
        int maxCount = rule.getCount() != null ? rule.getCount() : Integer.MAX_VALUE;
        int interval = rule.getInterval() != null ? rule.getInterval() : 1;
        int generated = 1; // 원본이 인스턴스 #1

        List<LocalDate> all = new ArrayList<>();

        switch (rule.getFreq()) {
            case DAILY -> {
                LocalDate cur = originalDate.plusDays(interval);
                while (!cur.isAfter(searchEnd) && generated < maxCount) {
                    all.add(cur);
                    cur = cur.plusDays(interval);
                    generated++;
                }
            }
            case WEEKLY -> {
                List<DayOfWeek> days = parseByDayWeekly(rule.getByDay(), originalDate.getDayOfWeek());
                LocalDate cycleStart = originalDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                while (!cycleStart.isAfter(searchEnd) && generated < maxCount) {
                    for (DayOfWeek dow : days) {
                        if (generated >= maxCount) break;
                        LocalDate candidate = cycleStart.with(TemporalAdjusters.nextOrSame(dow));
                        if (candidate.isAfter(originalDate) && !candidate.isAfter(searchEnd)) {
                            all.add(candidate);
                            generated++;
                        }
                    }
                    cycleStart = cycleStart.plusWeeks(interval);
                }
            }
            case MONTHLY -> {
                LocalDate cur = originalDate.plusMonths(interval);
                while (!cur.isAfter(searchEnd) && generated < maxCount) {
                    if (rule.getByDay() != null && !rule.getByDay().isBlank()) {
                        String byDay = rule.getByDay();
                        LocalDate occ;
                        if (Character.isDigit(byDay.charAt(0))) {
                            // "2WED" 형식: N번째 요일 직접 지정
                            occ = parseNthWeekdayOfMonth(cur, byDay);
                        } else {
                            // "TUE" 형식: 원본 날짜 기준으로 몇 번째 요일인지 자동 계산
                            DayOfWeek dow = toDayOfWeek(byDay);
                            int nth = (originalDate.getDayOfMonth() - 1) / 7 + 1;
                            occ = cur.withDayOfMonth(1).with(TemporalAdjusters.dayOfWeekInMonth(nth, dow));
                        }
                        if (!occ.isAfter(searchEnd)) { all.add(occ); generated++; }
                    } else {
                        for (int day : parseByMonthDay(rule.getByMonthDay(), originalDate.getDayOfMonth())) {
                            if (generated >= maxCount) break;
                            LocalDate occ = cur.withDayOfMonth(Math.min(day, cur.lengthOfMonth()));
                            if (!occ.isAfter(searchEnd)) { all.add(occ); generated++; }
                        }
                    }
                    cur = cur.plusMonths(interval);
                }
            }
            case YEARLY -> {
                LocalDate cur = originalDate.plusYears(interval);
                while (!cur.isAfter(searchEnd) && generated < maxCount) {
                    all.add(cur);
                    cur = cur.plusYears(interval);
                    generated++;
                }
            }
        }

        // 원본 날짜는 DB 조회에 이미 포함되어 있으므로 제외하고 범위 필터링
        return all.stream()
                .filter(d -> !d.isBefore(rangeStart) && !d.isAfter(rangeEnd))
                .toList();
    }

    private List<DayOfWeek> parseByDayWeekly(String byDay, DayOfWeek defaultDay) {
        if (byDay == null || byDay.isBlank()) return List.of(defaultDay);
        return Arrays.stream(byDay.split(","))
                .map(String::trim)
                .map(this::toDayOfWeek)
                .distinct()
                .sorted(Comparator.comparingInt(DayOfWeek::getValue))
                .toList();
    }

    private List<Integer> parseByMonthDay(String byMonthDay, int defaultDay) {
        if (byMonthDay == null || byMonthDay.isBlank()) return List.of(defaultDay);
        return Arrays.stream(byMonthDay.split(","))
                .map(String::trim)
                .map(Integer::parseInt)
                .sorted()
                .toList();
    }

    private LocalDate parseNthWeekdayOfMonth(LocalDate monthBase, String byDay) {
        // "2WED" → 숫자(N번째) + 요일 약자
        int nth = Character.getNumericValue(byDay.charAt(0));
        DayOfWeek dow = toDayOfWeek(byDay.substring(1));
        return monthBase.withDayOfMonth(1).with(TemporalAdjusters.dayOfWeekInMonth(nth, dow));
    }

    private DayOfWeek toDayOfWeek(String abbr) {
        return switch (abbr.toUpperCase()) {
            case "SUN" -> DayOfWeek.SUNDAY;
            case "MON" -> DayOfWeek.MONDAY;
            case "TUE" -> DayOfWeek.TUESDAY;
            case "WED" -> DayOfWeek.WEDNESDAY;
            case "THU" -> DayOfWeek.THURSDAY;
            case "FRI" -> DayOfWeek.FRIDAY;
            case "SAT" -> DayOfWeek.SATURDAY;
            default -> throw new IllegalArgumentException("Unknown day abbreviation: " + abbr);
        };
    }
}