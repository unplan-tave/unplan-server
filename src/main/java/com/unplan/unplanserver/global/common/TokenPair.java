package com.unplan.unplanserver.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public record TokenPair (
        String accessToken,
        String refreshToken
){
}
