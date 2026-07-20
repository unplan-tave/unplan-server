package com.unplan.unplanserver.global.config;

import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000) //TCP 연결 타임아웃 3초
                .responseTimeout(Duration.ofSeconds(5));
        return WebClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
