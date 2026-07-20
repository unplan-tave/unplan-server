package com.unplan.unplanserver.domain.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleDetailResponse;
import com.unplan.unplanserver.domain.schedule.entity.Schedule;
import com.unplan.unplanserver.domain.schedule.enums.ConditionTag;
import com.unplan.unplanserver.domain.schedule.enums.ScheduleStatus;
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

/**
 * 일정 수정 시 개인 태그 전체 교체(detach 후 attach) 경로 검증.
 * 기존과 동일한 태그로 다시 저장하면, 조인행을 지웠다가 같은 (schedule, tag) 를 재삽입하는데
 * Hibernate 가 insert 를 delete 보다 먼저 flush 하면 uk_schedule_personal_tag 위반으로 500 이 났었다(회귀 방지).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleUpdateTagIntegrationTest {

    private static final Long MEMBER_ID = 9500L;

    @Autowired private ScheduleService scheduleService;
    @Autowired private ScheduleRepository scheduleRepository;
    @Autowired private TagService tagService;
    @PersistenceContext private EntityManager em;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    @DisplayName("기존과 같은 개인 태그로 다시 수정해도 유니크 제약 위반 없이 저장된다")
    void updateWithSameTagDoesNotViolateUniqueConstraint() throws Exception {
        Schedule s = scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID).title("보고서").conditionTag(ConditionTag.CORE_TASK)
                .date(LocalDate.of(2026, 6, 20)).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .isQueue(false).isRecurring(false).isConflict(false)
                .status(ScheduleStatus.TODO).build());
        tagService.attachTags(s, MEMBER_ID, List.of("과제", "중요"));
        em.flush();
        em.clear();

        // 태그를 그대로 유지한 채 재저장 (detach 전체 → 같은 태그 재attach)
        ScheduleUpdateRequest req = objectMapper.readValue(
                "{\"personal_tags\":[\"과제\",\"중요\"]}", ScheduleUpdateRequest.class);
        ScheduleDetailResponse res = scheduleService.updateSchedule(MEMBER_ID, s.getScheduleId(), req);

        // 실제 운영처럼 flush 를 강제해 제약 위반 여부를 즉시 드러낸다 (없어야 통과)
        em.flush();

        assertThat(res.getPersonalTags()).containsExactlyInAnyOrder("과제", "중요");
    }

    @Test
    @DisplayName("제목과 태그를 함께 수정하면 태그 삭제 벌크쿼리에도 일정 제목 변경이 유실되지 않는다")
    void updateFieldsAndTagsTogetherPersistsFieldChanges() throws Exception {
        // 큐 카드(시간 없음): updateSchedule 의 시간겹침 검증이 early-return 이라 detachAll 전에 auto-flush 가 없다.
        // 이때 태그 삭제 벌크쿼리가 영속성 컨텍스트를 clear 하면 아직 flush 안 된 제목 변경이 유실된다(회귀 방지).
        Schedule s = scheduleRepository.save(Schedule.builder()
                .memberId(MEMBER_ID).title("보고서").conditionTag(ConditionTag.CORE_TASK)
                .date(LocalDate.of(2026, 6, 20)).estimatedTime(30)
                .isQueue(true).isRecurring(false).isConflict(false)
                .status(ScheduleStatus.TODO).build());
        tagService.attachTags(s, MEMBER_ID, List.of("과제"));
        em.flush();
        em.clear();

        // 제목 변경 + 같은 태그 유지 (시간은 그대로 없음 → 큐 카드 유지)
        ScheduleUpdateRequest req = objectMapper.readValue(
                "{\"title\":\"수정된 보고서\",\"personal_tags\":[\"과제\"]}", ScheduleUpdateRequest.class);
        scheduleService.updateSchedule(MEMBER_ID, s.getScheduleId(), req);
        em.flush();
        em.clear();

        // 새 영속성 컨텍스트에서 실제 DB 값 확인 — 제목 변경이 저장돼 있어야 한다
        Schedule reloaded = scheduleRepository.findById(s.getScheduleId()).orElseThrow();
        assertThat(reloaded.getTitle()).isEqualTo("수정된 보고서");
    }
}
