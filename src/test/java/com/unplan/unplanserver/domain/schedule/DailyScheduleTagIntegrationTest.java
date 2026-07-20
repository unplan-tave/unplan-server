package com.unplan.unplanserver.domain.schedule;

import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleGetResponse;
import com.unplan.unplanserver.domain.schedule.entity.RecurrenceRule;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.RecurrenceFreq;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
import com.unplan.unplanserver.domain.schedule.repository.RecurrenceRuleRepository;
import com.unplan.unplanserver.domain.schedule.repository.ScheduleRepository;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import com.unplan.unplanserver.domain.schedule.service.TagService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * 일별 조회 응답에 개인 태그가 담기는지 검증한다.
 * 홈 화면이 컨디션 태그와 함께 개인 태그도 보여줘야 하므로, 일별 조회가 태그를 반환해야 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DailyScheduleTagIntegrationTest {

    private static final Long MEMBER_ID = 9200L;
    private static final LocalDate DATE = LocalDate.of(2026, 6, 20);

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private RecurrenceRuleRepository recurrenceRuleRepository;
    @Autowired private TagService tagService;
    @PersistenceContext private EntityManager em;

    @Test
    @DisplayName("일별 조회 응답에 각 일정의 개인 태그가 담기고, 태그가 없으면 빈 목록이다")
    void dailyResponseContainsPersonalTags() {
        Schedule tagged = saveSchedule("태그있음", false);
        tagService.attachTags(tagged, MEMBER_ID, List.of("업무", "중요"));
        Schedule untagged = saveSchedule("태그없음", false);
        em.flush();
        em.clear();

        List<ScheduleGetResponse> result = scheduleService.getSchedulesByDate(MEMBER_ID, DATE);

        assertThat(result)
                .extracting(ScheduleGetResponse::getTitle, ScheduleGetResponse::getPersonalTags)
                .contains(
                        tuple("태그있음", List.of("업무", "중요")),
                        tuple("태그없음", List.of()));
    }

    @Test
    @DisplayName("반복 일정 인스턴스에도 원본의 개인 태그가 그대로 담긴다")
    void recurringInstanceCarriesOriginalTags() {
        Schedule original = saveSchedule("반복태그", true);
        tagService.attachTags(original, MEMBER_ID, List.of("루틴"));
        recurrenceRuleRepository.save(RecurrenceRule.builder()
                .schedule(original).freq(RecurrenceFreq.DAILY).interval(1).build());
        em.flush();
        em.clear();

        // 원본 시작일 다음 날 조회 → 반복 인스턴스만 잡힌다
        List<ScheduleGetResponse> result = scheduleService.getSchedulesByDate(MEMBER_ID, DATE.plusDays(1));

        assertThat(result)
                .extracting(ScheduleGetResponse::getTitle, ScheduleGetResponse::getPersonalTags)
                .containsExactly(tuple("반복태그", List.of("루틴")));
    }

    private Schedule saveSchedule(String title, boolean recurring) {
        return scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID).title(title).conditionTag(ConditionTag.CORE_TASK)
                .date(DATE).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .isQueue(false).isRecurring(recurring).isConflict(false)
                .status(ScheduleStatus.TODO).build());
    }
}
