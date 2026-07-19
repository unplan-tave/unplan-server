package com.unplan.unplanserver.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.unplan.unplanserver.domain.auth.dto.GoogleUserInfoDto;
import com.unplan.unplanserver.domain.member.entity.Member;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

// 구글 id 토큰을 검증하는 클래스
@Component
@RequiredArgsConstructor
public class GoogleIdTokenValidator {
    @Value("${google.client-id}")
    private String googleClientId;
    private static final HttpTransport transport = new NetHttpTransport();
    private static final JsonFactory jsonFactory = new GsonFactory();
    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        // 타임아웃관련 설정추가
        HttpRequestFactory requestFactory = transport.createRequestFactory(
                request->{
                    request.setConnectTimeout(3000);
                    request.setReadTimeout(5000);
                }
        );

        this.verifier = new GoogleIdTokenVerifier.Builder(requestFactory.getTransport(), jsonFactory)
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }
    public GoogleUserInfoDto isValid(String googleIdToken) {
        try{
            GoogleIdToken idToken = verifier.verify(googleIdToken);
            if(idToken != null){
                GoogleIdToken.Payload payload = idToken.getPayload();
                String oauthId = payload.getSubject();
                String nickname = (String) payload.get("name");
                String email = payload.getEmail();
                GoogleUserInfoDto googleUserInfoDto = new GoogleUserInfoDto(oauthId, nickname, email);
                return googleUserInfoDto;
            }
            else{
                throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
            }
        } catch (GeneralSecurityException e) {
            throw new CustomException(ErrorCode.INVALID_GOOGLE_TOKEN);
        } catch (IOException e) {
            if (e.getCause() instanceof java.net.ConnectException) {
                throw new CustomException(ErrorCode.GOOGLE_CONNECTION_TIMEOUT);
            }
            if (e.getCause() instanceof java.net.SocketTimeoutException) {
                throw new CustomException(ErrorCode.GOOGLE_RESPONSE_TIMEOUT);
            }
            throw new CustomException(ErrorCode.GOOGLE_SERVER_ERROR);
        }
    }
}
