package com.unplan.unplanserver.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class GoogleUserInfoDto {
    private String oauthId;
    private String nickname;
    private String email;
}
