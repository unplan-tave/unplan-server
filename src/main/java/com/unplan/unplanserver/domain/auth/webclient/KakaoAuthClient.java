package com.unplan.unplanserver.domain.auth.webclient;

import com.unplan.unplanserver.domain.auth.dto.KakaoUserInfoResponseDto;
import com.unplan.unplanserver.global.exception.KakaoServerException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KakaoAuthClient {
    private final WebClient webClient;

    public KakaoUserInfoResponseDto getUserInfo(String kakaoAccessToken) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new KakaoServerException()))
                .bodyToMono(KakaoUserInfoResponseDto.class)// json->java객체
                .block();
    }
}
