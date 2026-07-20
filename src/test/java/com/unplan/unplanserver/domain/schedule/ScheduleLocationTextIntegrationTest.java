package com.unplan.unplanserver.domain.schedule;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleCreateRequest;
import com.unplan.unplanserver.domain.schedule.dto.request.ScheduleUpdateRequest;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleCreateResponse;
import com.unplan.unplanserver.domain.schedule.dto.response.ScheduleDetailResponse;
import com.unplan.unplanserver.domain.schedule.service.ScheduleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 일정의 위치 텍스트(location) 저장·조회 검증.
 * 기존에는 좌표(latitude/longitude)만 받아 사용자가 입력한 장소 텍스트를 저장할 수 없었다 —
 * 생성/수정 요청에 location 을 받아 저장하고 상세 조회에서 그대로 보여줘야 한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScheduleLocationTextIntegrationTest {

    private static final Long MEMBER_ID = 9400L;

    @Autowired private ScheduleService scheduleService;

    // 요청 DTO는 빌더/세터가 없어 JSON 역직렬화로 구성. 앱과 동일하게 snake_case + JavaTime.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    @DisplayName("생성 시 입력한 위치 텍스트가 저장되고 상세 조회에서 보인다")
    void createStoresLocationText() throws Exception {
        ScheduleCreateRequest req = objectMapper.readValue("""
                {"title":"미팅","condition_tag":"CORE_TASK","date":"2026-06-20",
                 "location":"강남역 스타벅스","is_remind_on":false}
                """, ScheduleCreateRequest.class);

        ScheduleCreateResponse created = scheduleService.createSchedule(MEMBER_ID, req);
        ScheduleDetailResponse detail = scheduleService.getScheduleDetail(MEMBER_ID, created.getScheduleId());

        assertThat(detail.getLocation()).isEqualTo("강남역 스타벅스");
    }

    @Test
    @DisplayName("수정 시 위치 텍스트가 갱신된다")
    void updateChangesLocationText() throws Exception {
        ScheduleCreateRequest createReq = objectMapper.readValue("""
                {"title":"미팅","condition_tag":"CORE_TASK","date":"2026-06-20",
                 "location":"강남역","is_remind_on":false}
                """, ScheduleCreateRequest.class);
        Long scheduleId = scheduleService.createSchedule(MEMBER_ID, createReq).getScheduleId();

        ScheduleUpdateRequest updateReq = objectMapper.readValue(
                "{\"location\":\"홍대입구역 2번 출구\"}", ScheduleUpdateRequest.class);
        scheduleService.updateSchedule(MEMBER_ID, scheduleId, updateReq);

        ScheduleDetailResponse detail = scheduleService.getScheduleDetail(MEMBER_ID, scheduleId);
        assertThat(detail.getLocation()).isEqualTo("홍대입구역 2번 출구");
    }
}
