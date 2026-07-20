package com.unplan.unplanserver.domain.schedule.service;

import com.unplan.unplanserver.domain.measurement.dto.response.MeasurementRecordResponse;
import com.unplan.unplanserver.domain.measurement.entity.Condition;
import com.unplan.unplanserver.domain.measurement.entity.Sleep;
import com.unplan.unplanserver.domain.measurement.repository.ConditionRepository;
import com.unplan.unplanserver.domain.measurement.repository.SleepRepository;
import com.unplan.unplanserver.domain.measurement.service.MeasurementService;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.unplan.unplanserver.domain.schedule.dto.response.*;
import com.unplan.unplanserver.domain.schedule.entity.LocationInfo;
import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.DailyMessage;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.LocationInfoRepository;
import com.unplan.unplanserver.domain.schedule.repository.RecurrenceRuleRepository;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import com.unplan.unplanserver.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final LocationInfoRepository locationInfoRepository;
    private final RecurrenceRuleRepository recurrenceRuleRepository;
    private final TagService tagService;
    private final MeasurementService measurementService;

    @Transactional
    public ScheduleCreateResponse createSchedule(Long memberId, ScheduleCreateRequest request) {

        // 0. 반복 설정 형식 검증 (잘못된 규칙이 저장되어 이후 조회를 깨뜨리는 것을 방지)
        if (request.getRecurrence() != null) {
            validateRecurrence(request.getRecurrence());
        }
        // 시작/종료 시간 검증 — 한쪽만 있는 '반쪽 핀 카드'나 역전된 구간이 저장되면
        // 추천 빈 시간 계산(busy 매핑)이 깨지므로 저장 전에 차단한다.
        validateTimePair(request.getStartTime(), request.getEndTime());
        // 핀 카드는 같은 날짜의 기존 핀 카드(반복 인스턴스 포함)와 시간이 겹치면 안 된다.
        validatePinNotOverlapping(memberId, request.getDate(),
                request.getStartTime(), request.getEndTime(), null);

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
                .location(request.getLocation())
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

        // 4. 개인 태그 생성 및 연결 (TagService에 위임: find-or-create 후 조인 테이블 연결)
        List<String> linkedTags = tagService.attachTags(saved, memberId, request.getPersonalTags());

        // 5. Response 반환
        return ScheduleCreateResponse.builder()
                .scheduleId(saved.getScheduleId())
                .title(saved.getTitle())
                .date(saved.getDate() != null ? saved.getDate().toString() : null)
                .startTime(saved.getStartTime() != null ? saved.getStartTime().toString() : null)
                .endTime(saved.getEndTime() != null ? saved.getEndTime().toString() : null)
                .estimatedTime(saved.getEstimatedTime())
                .isQueue(saved.getIsQueue())
                .personalTags(linkedTags)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ScheduleGetResponse> getSchedulesByDate(Long memberId, LocalDate date) {
        List<Schedule> schedules = findSchedulesWithRecurring(memberId, date);
        // 개인 태그를 일정 id 기준으로 한 번에 조회해 매핑 (건별 조회 N+1 방지).
        // 반복 인스턴스는 원본과 같은 scheduleId 를 갖고 있어 원본 태그가 그대로 매핑된다.
        Map<Long, List<String>> tagsByScheduleId = tagService.getTagNamesByScheduleIds(
                schedules.stream().map(Schedule::getScheduleId).toList());
        return schedules.stream()
                .map(s -> ScheduleGetResponse.from(
                        s, tagsByScheduleId.getOrDefault(s.getScheduleId(), List.of())))
                .toList();
    }

    /**
     * 특정 날짜의 일정 목록 (반복 일정의 해당 날짜 인스턴스 포함, 엔티티 반환).
     * 추천 모듈이 핀 카드 점유 시간(busy) 계산에 사용한다 — 반복 핀 카드도 빈 시간에서 제외되어야 하므로.
     */
    @Transactional(readOnly = true)
    public List<Schedule> findSchedulesWithRecurring(Long memberId, LocalDate date) {
        List<Schedule> schedules = new ArrayList<>(scheduleRepository.findByMemberIdAndDate(memberId, date));
        schedules.addAll(expandRecurringInstances(memberId, date, date));
        return schedules;
    }

    @Transactional(readOnly = true)
    public ScheduleDetailResponse getScheduleDetail(Long memberId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findByScheduleIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        LocationInfo locationInfo = locationInfoRepository.findBySchedule(schedule).orElse(null);
        RecurrenceRule recurrenceRule = recurrenceRuleRepository.findBySchedule(schedule).orElse(null);
        return ScheduleDetailResponse.from(schedule, locationInfo,
                tagService.getTagNamesBySchedule(schedule), recurrenceRule);
    }

    @Transactional(readOnly = true)
    public List<PersonalTagResponse> getPersonalTags(Long memberId) {
        return tagService.getPersonalTags(memberId).stream()
                .map(PersonalTagResponse::from)
                .toList();
    }

    @Transactional
    public ScheduleDetailResponse updateSchedule(Long memberId, Long scheduleId, ScheduleUpdateRequest request) {
        Schedule schedule = scheduleRepository.findByScheduleIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        schedule.update(request);
        // 부분 수정(PATCH) 결과가 반쪽 핀 카드/역전 구간이 되지 않는지 최종 상태로 검증.
        // 검증 실패 시 예외로 트랜잭션이 롤백되어 변경이 반영되지 않는다.
        validateTimePair(schedule.getStartTime(), schedule.getEndTime());
        // 수정 후에도 같은 날짜의 다른 핀 카드(반복 인스턴스 포함)와 시간이 겹치면 안 된다(자기 자신 제외).
        validatePinNotOverlapping(memberId, schedule.getDate(),
                schedule.getStartTime(), schedule.getEndTime(), scheduleId);

        // personalTags가 요청에 포함된 경우에만 태그 전체 교체 (null = 기존 유지, 빈 배열 = 전체 해제)
        List<String> tagNames;
        if (request.getPersonalTags() != null) {
            tagService.detachAll(schedule);
            tagNames = tagService.attachTags(schedule, memberId, request.getPersonalTags());
        } else {
            tagNames = tagService.getTagNamesBySchedule(schedule);
        }

        LocationInfo locationInfo = locationInfoRepository.findBySchedule(schedule).orElse(null);
        RecurrenceRule recurrenceRule = recurrenceRuleRepository.findBySchedule(schedule).orElse(null);
        return ScheduleDetailResponse.from(schedule, locationInfo, tagNames, recurrenceRule);
    }

    @Transactional
    public void deleteSchedule(Long memberId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findByScheduleIdAndMemberId(scheduleId, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.SCHEDULE_NOT_FOUND));
        // schedule_id 를 FK 로 참조하는 행들을 먼저 정리해 FK 위반 방지 (#120)
        tagService.detachAll(schedule);
        recurrenceRuleRepository.deleteBySchedule(schedule);
        locationInfoRepository.deleteBySchedule(schedule);
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

    // 시작/종료 시간은 둘 다 있거나(핀 카드) 둘 다 없어야(큐 카드) 하고, 있으면 시작 < 종료.
    // 일정이 단일 date 에 귀속되는 모델이라 자정을 넘기는 구간(start > end)은 표현할 수 없다.
    private void validateTimePair(LocalTime startTime, LocalTime endTime) {
        if ((startTime == null) != (endTime == null)) {
            throw new CustomException(ErrorCode.INVALID_SCHEDULE_TIME);
        }
        if (startTime != null && !startTime.isBefore(endTime)) {
            throw new CustomException(ErrorCode.INVALID_SCHEDULE_TIME);
        }
    }

    /**
     * 핀 카드 시간 겹침 검증 — 같은 날짜에 시간이 1분이라도 겹치는 핀 카드가 있으면 등록/수정을 막는다.
     * 반복 일정의 해당 날짜 인스턴스도 점유 시간으로 간주한다({@code findSchedulesWithRecurring} 가 함께 반환).
     * 큐 카드(시간 미지정)는 겹침 대상이 아니며, 수정 시에는 자기 자신({@code excludeId})을 제외한다.
     * 맞닿는 경계(예: 10:00~11:00 과 11:00~12:00)는 겹침으로 보지 않는다.
     */
    private void validatePinNotOverlapping(Long memberId, LocalDate date,
                                           LocalTime start, LocalTime end, Long excludeId) {
        if (start == null || end == null || date == null) return; // 핀 카드가 아니면 겹침 없음
        for (Schedule other : findSchedulesWithRecurring(memberId, date)) {
            if (excludeId != null && excludeId.equals(other.getScheduleId())) continue; // 자기 자신(반복 인스턴스 포함)
            if (other.getStartTime() == null || other.getEndTime() == null) continue;   // 큐 카드 제외
            if (start.isBefore(other.getEndTime()) && other.getStartTime().isBefore(end)) {
                throw new CustomException(ErrorCode.TIME_RANGE_OVERLAP);
            }
        }
    }

    // 반복 규칙 형식 검증 — 잘못된 값이 저장되어 이후 조회 확장 시 예외를 일으키는 것을 차단
    private void validateRecurrence(ScheduleCreateRequest.RecurrenceRequest rec) {
        if (rec.getInterval() != null && rec.getInterval() < 1) {
            throw new CustomException(ErrorCode.INVALID_RECURRENCE);
        }

        // by_month_day: 1~31 정수 토큰만 허용 (예: "16", "1,17")
        if (rec.getByMonthDay() != null && !rec.getByMonthDay().isBlank()) {
            for (String token : rec.getByMonthDay().split(",")) {
                token = token.trim();
                if (token.isEmpty()) continue;
                int day;
                try {
                    day = Integer.parseInt(token);
                } catch (NumberFormatException e) {
                    throw new CustomException(ErrorCode.INVALID_RECURRENCE);
                }
                if (day < 1 || day > 31) {
                    throw new CustomException(ErrorCode.INVALID_RECURRENCE);
                }
            }
        }

        // by_day: 요일 토큰 (MONTHLY는 선행 숫자 N번째 허용, 예: "2WED")
        if (rec.getByDay() != null && !rec.getByDay().isBlank()) {
            boolean monthly = rec.getFreq() == RecurrenceFreq.MONTHLY;
            for (String token : rec.getByDay().split(",")) {
                token = token.trim();
                if (token.isEmpty()) continue;
                String dayPart = token;
                if (monthly && Character.isDigit(token.charAt(0))) {
                    // N번째 요일은 1~5만 유효 ("0WED"는 전월로 넘어가는 미정의 동작이 됨)
                    int nth = Character.getNumericValue(token.charAt(0));
                    if (nth < 1 || nth > 5) {
                        throw new CustomException(ErrorCode.INVALID_RECURRENCE);
                    }
                    dayPart = token.substring(1);
                }
                if (!isValidWeekday(dayPart)) {
                    throw new CustomException(ErrorCode.INVALID_RECURRENCE);
                }
            }
        }
    }

    private boolean isValidWeekday(String abbr) {
        return switch (abbr.toUpperCase()) {
            case "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT" -> true;
            default -> false;
        };
    }

    private List<Schedule> expandRecurringInstances(Long memberId, LocalDate rangeStart, LocalDate rangeEnd) {
        // 조회 범위에 인스턴스가 생길 수 있는 활성 반복만 규칙+원본을 단일 쿼리로 조회 (시작일<=rangeEnd, until null 이거나 >=rangeStart)
        List<RecurrenceRule> rules =
                recurrenceRuleRepository.findActiveRulesWithSchedule(memberId, rangeStart, rangeEnd);

        List<Schedule> expanded = new ArrayList<>();
        for (RecurrenceRule rule : rules) {
            Schedule original = rule.getSchedule();

            // 규칙 하나가 깨져도(과거에 저장된 비정상 데이터 등) 전체 조회가 실패하지 않도록 방어
            try {
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
            } catch (RuntimeException e) {
                log.warn("반복 일정 확장 실패로 건너뜀 (scheduleId={}): {}", original.getScheduleId(), e.getMessage());
            }
        }
        return expanded;
    }

    private List<LocalDate> calculateInstancesInRange(LocalDate originalDate, RecurrenceRule rule,
                                                      LocalDate rangeStart, LocalDate rangeEnd) {
        LocalDate searchEnd = (rule.getUntil() != null && rule.getUntil().isBefore(rangeEnd))
                ? rule.getUntil() : rangeEnd;
        int maxCount = rule.getCount() != null ? rule.getCount() : Integer.MAX_VALUE;
        // interval 이 0/음수면 plusDays/Weeks/Months/Years(0) 으로 커서가 멈춰 무한 루프가 된다.
        // 생성 시 validateRecurrence 로 막지만, 레거시·비정상 데이터 방어를 위해 조회 경로에서도 1 이상으로 클램핑.
        int interval = (rule.getInterval() != null && rule.getInterval() >= 1) ? rule.getInterval() : 1;
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
                // 주별/월별 뷰와 동일하게 일요일을 주 시작으로 통일 (interval>=2 일 때 주 경계 일관성 유지)
                LocalDate cycleStart = originalDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
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
                // 원본 달부터 시작하되 원본 날짜 이후 인스턴스만 채택 (다중 byMonthDay의 원본 달 누락 방지)
                LocalDate cur = originalDate.withDayOfMonth(1);
                while (!cur.isAfter(searchEnd) && generated < maxCount) {
                    for (LocalDate occ : monthlyOccurrences(cur, originalDate, rule)) {
                        if (generated >= maxCount) break;
                        if (occ.isAfter(originalDate) && !occ.isAfter(searchEnd)) {
                            all.add(occ);
                            generated++;
                        }
                    }
                    cur = cur.plusMonths(interval);
                }
            }
            case YEARLY -> {
                // 매번 원본에서 계산해 윤년 2/29 드리프트 방지 (plusYears가 평년엔 2/28로 자동 보정)
                for (int n = 1; generated < maxCount; n++) {
                    LocalDate cur = originalDate.plusYears((long) interval * n);
                    if (cur.isAfter(searchEnd)) break;
                    all.add(cur);
                    generated++;
                }
            }
        }

        // 원본 날짜는 DB 조회에 이미 포함되어 있으므로 제외하고 범위 필터링.
        // sorted: WEEKLY 다중 요일(일요일 앵커에서 SUN이 다른 요일보다 늦게 추가되는 등) 생성 순서가
        //         연대순이 아닐 수 있어, 반환 목록이 항상 오름차순임을 보장한다.
        return all.stream()
                .filter(d -> !d.isBefore(rangeStart) && !d.isAfter(rangeEnd))
                .sorted()
                .toList();
    }

    // monthBase가 속한 달 안에서 반복 규칙에 해당하는 날짜들을 계산.
    // byMonthDay 클램핑으로 같은 말일에 겹치는 경우(예: "30,31" → 2월 28일) distinct로 중복 제거.
    private List<LocalDate> monthlyOccurrences(LocalDate monthBase, LocalDate originalDate, RecurrenceRule rule) {
        List<LocalDate> result = new ArrayList<>();
        if (rule.getByDay() != null && !rule.getByDay().isBlank()) {
            for (String token : rule.getByDay().split(",")) {
                token = token.trim();
                if (token.isEmpty()) continue;
                if (Character.isDigit(token.charAt(0))) {
                    // "2WED" 형식: N번째 요일 직접 지정
                    result.add(parseNthWeekdayOfMonth(monthBase, token));
                } else {
                    // "TUE" 형식: 원본 날짜 기준으로 몇 번째 요일인지 자동 계산
                    DayOfWeek dow = toDayOfWeek(token);
                    int nth = (originalDate.getDayOfMonth() - 1) / 7 + 1;
                    result.add(nthWeekdayInMonth(monthBase, nth, dow));
                }
            }
        } else {
            for (int day : parseByMonthDay(rule.getByMonthDay(), originalDate.getDayOfMonth())) {
                // 해당 일자가 그 달에 없으면 말일로 당김 (lengthOfMonth가 윤년 자동 반영)
                result.add(monthBase.withDayOfMonth(Math.min(day, monthBase.lengthOfMonth())));
            }
        }
        return result.stream().distinct().sorted().toList();
    }

    private List<DayOfWeek> parseByDayWeekly(String byDay, DayOfWeek defaultDay) {
        if (byDay == null || byDay.isBlank()) return List.of(defaultDay);
        List<DayOfWeek> days = Arrays.stream(byDay.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty()) // 끝/중복 콤마로 생기는 빈 토큰 방어
                .map(this::toDayOfWeek)
                .distinct()
                // 주기 앵커(일요일)와 같은 순서로 정렬해야 count 절단이 연대순으로 적용된다.
                // ISO 값(월=1…일=7) 정렬을 쓰면 일요일이 마지막에 생성되어, count 초과 시
                // 주 안에서 가장 이른 일요일 인스턴스가 잘리고 더 늦은 요일이 남는 오류가 생긴다.
                .sorted(Comparator.comparingInt(dow -> dow.getValue() % 7)) // SUN=0, MON=1 … SAT=6
                .toList();
        return days.isEmpty() ? List.of(defaultDay) : days;
    }

    private List<Integer> parseByMonthDay(String byMonthDay, int defaultDay) {
        if (byMonthDay == null || byMonthDay.isBlank()) return List.of(defaultDay);
        return Arrays.stream(byMonthDay.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty()) // 끝/중복 콤마로 생기는 빈 토큰 방어
                .map(Integer::parseInt)
                .filter(d -> d >= 1 && d <= 31) // 0·음수·32+ 같은 범위 밖 값 제거 (withDayOfMonth 예외 방지)
                .distinct()
                .sorted()
                .toList();
    }

    private LocalDate parseNthWeekdayOfMonth(LocalDate monthBase, String byDay) {
        // "2WED" → 숫자(N번째) + 요일 약자
        int nth = Character.getNumericValue(byDay.charAt(0));
        DayOfWeek dow = toDayOfWeek(byDay.substring(1));
        return nthWeekdayInMonth(monthBase, nth, dow);
    }

    // N번째 요일이 해당 월에 없으면(다음 달로 overflow) 마지막 주차로 폴백
    private LocalDate nthWeekdayInMonth(LocalDate monthBase, int nth, DayOfWeek dow) {
        LocalDate firstOfMonth = monthBase.withDayOfMonth(1);
        LocalDate candidate = firstOfMonth.with(TemporalAdjusters.dayOfWeekInMonth(nth, dow));
        if (!candidate.getMonth().equals(monthBase.getMonth())) {
            candidate = firstOfMonth.with(TemporalAdjusters.lastInMonth(dow));
        }
        return candidate;
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

    @Transactional(readOnly = true)
    public DailyMessageResponseDto getDailyMessage(Long memberId, LocalDate date) {
        MeasurementRecordResponse dailyRecord = measurementService.getDailyRecord(memberId, date);
        ConditionTag conditionTag = ConditionTag.fromLabel(dailyRecord.conditionTag());
        DailyMessage dailyMessage = DailyMessage.fromConditionTag(conditionTag);
        String message = dailyMessage.getPrescription() + "\n" + dailyMessage.getSuggestion();
        if (!dailyRecord.isEnergyRecorded() && !dailyRecord.isSleepRecorded()) {
            message = "컨디션을 기록하고 나에게 딱 맞는 일정을 추천 받아보세요!";
        }
        return new DailyMessageResponseDto(
                date,
                dailyMessage.getConditionTag(),
                message,
                dailyRecord.isEnergyRecorded(),
                dailyRecord.isSleepRecorded()
        );


    }
}