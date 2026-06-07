package com.unplan.unplanserver.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
public class KakaoUserInfoResponseDto {

    private Long id;

    private KakaoAccount kakaoAccount;

    @Getter
    public static class KakaoAccount {

        private Boolean profileNicknameNeedsAgreement;
        private Profile profile;

        private Boolean emailNeedsAgreement;
        private Boolean isEmailValid;
        private Boolean isEmailVerified;
        private String email;

        @Getter
        public static class Profile {
            private String nickname;
        }
    }
}