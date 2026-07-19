package com.unplan.unplanserver.webclient;

import com.unplan.unplanserver.domain.auth.dto.response.KakaoUserInfoResponseDto;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class KakaoAuthClient {
    private final WebClient webClient;

    public KakaoUserInfoResponseDto getUserInfo(String kakaoAccessToken) {
        return webClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(status -> status.value() == 401, response -> Mono.error(new CustomException(ErrorCode.INVALID_KAKAO_TOKEN)))
                .onStatus(HttpStatusCode::isError, response -> Mono.error(new CustomException(ErrorCode.KAKAO_SERVER_ERROR)))
                .bodyToMono(KakaoUserInfoResponseDto.class)// json->java객체
                .timeout(Duration.ofSeconds(5))
                .onErrorMap(io.netty.channel.ConnectTimeoutException.class, e->new CustomException(ErrorCode.KAKAO_CONNECTION_TIMEOUT))
                .onErrorMap(TimeoutException.class, e-> new CustomException(ErrorCode.KAKAO_RESPONSE_TIMEOUT))
                .block();
    }
}
