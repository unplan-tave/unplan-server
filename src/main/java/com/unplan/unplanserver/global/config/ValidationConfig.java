package com.unplan.unplanserver.global.config;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ValidationConfig {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Bean
    public ValidationConfigurationCustomizer kstValidationClock() {
        return configuration -> configuration.clockProvider(() -> Clock.system(KST_ZONE_ID));
    }
}
