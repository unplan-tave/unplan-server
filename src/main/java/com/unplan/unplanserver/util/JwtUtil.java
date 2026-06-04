package com.unplan.unplanserver.util;

import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    private static final SecretKey secretKey;
    private static final Long accessExpiration;
    private static final Long refreshExpiration;

    static{
        accessExpiration = 3600L * 1000;    // 1시간
        refreshExpiration = 3600L * 1000 * 24 * 30;  // 30일
    }

    public static String createJwt(Long memberId, Boolean isAccess){
        long now = System.currentTimeMillis();
        long expiry = isAccess ? accessExpiration : refreshExpiration;
        String type = isAccess ? "access" : "refresh";

        return Jwts.builder()
                .subject(memberId.toString())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiry))
                .signWith(secretKey)
                .compact();
    }
}
