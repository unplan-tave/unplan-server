package com.unplan.unplanserver.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unplan.unplanserver.domain.recommendation.dto.response.RecommendationAcceptResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * JacksonConfig 회귀 검증 — JavaTimeModule 미등록 시 LocalDate/LocalTime 응답이 500 나던 버그(#117).
 * 자바 8 날짜/시간이 ISO-8601 문자열 + snake_case 로 직렬화되는지 확인한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class JacksonConfigTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("LocalDate/LocalTime 을 담은 응답 DTO 가 500 없이 ISO 문자열로 직렬화된다")
    void serializesJavaTimeAsIsoStrings() {
        RecommendationAcceptResponse sample = new RecommendationAcceptResponse(
                1L, 2L, "제목",
                LocalDate.of(2026, 6, 1), LocalTime.of(10, 0), LocalTime.of(11, 0),
                "QUEUE_CARD", false);

        assertThatCode(() -> {
            String json = objectMapper.writeValueAsString(sample);
            assertThat(json)
                    .contains("\"date\":\"2026-06-01\"")           // LocalDate → ISO
                    .contains("\"start_time\":\"10:00:00\"")        // LocalTime → ISO(HH:mm:ss)
                    .contains("\"end_time\":\"11:00:00\"")
                    .contains("\"source_type\":\"QUEUE_CARD\"");    // snake_case
        }).doesNotThrowAnyException();
    }
}
