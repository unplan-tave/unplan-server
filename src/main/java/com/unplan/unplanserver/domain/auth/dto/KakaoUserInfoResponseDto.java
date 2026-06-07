package com.unplan.unplanserver.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
public class KakaoUserInfoResponseDto {

    private Long id;  // 카카오 고유 ID

    private KakaoAccount kakaoAccount;

    @Getter
    public static class KakaoAccount {

        private String email;
        private Profile profile;

        @Getter
        public static class Profile {
            private String nickname;
            private String profileImageUrl;
        }
    }
}