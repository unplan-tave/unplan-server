package com.unplan.unplanserver.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
    @Bean
    public ObjectMapper objectMapper() {
        // JavaTimeModule 미등록 시 LocalDate/LocalTime 응답이 직렬화 예외(500)가 나므로 명시 등록.
        // WRITE_DATES_AS_TIMESTAMPS 비활성 → 숫자 배열 대신 ISO-8601 문자열("2026-06-01", "10:00")로 직렬화.
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }
}
