package com.unplan.unplanserver.util;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration}") long accessExpiration,
            @Value("${jwt.refresh-expiration}") long refreshExpiration
    ){
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpiration = accessExpiration;    // 1시간
        this.refreshExpiration = refreshExpiration;  // 30일
    }

    public String createJwt(Long memberId, String role, Boolean isAccess){
        long now = System.currentTimeMillis();
        long expiry = isAccess ? accessExpiration : refreshExpiration;
        String type = isAccess ? "access" : "refresh";

        return Jwts.builder()
                .subject(memberId.toString())
                .claim("type", type)
                .claim("role", role)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiry))
                .signWith(secretKey)
                .compact();
    }
    public Boolean isValid(String token, Boolean isAccess) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)  // parser의 검증방식과 키 지정
                    .build()
                    .parseSignedClaims(token)   //만료시간검증은 자동
                    .getPayload();

            String type = claims.get("type", String.class);
            if (type == null) return false;
            if (isAccess && !"access".equals(type)) return false;
            if (!isAccess && !"refresh".equals(type)) return false;

            return true;
        }
        catch (ExpiredJwtException e){
            //만료된 토큰
            return false;
        }
        catch(JwtException | IllegalArgumentException e){
            // 위조된 토큰(JwtException)이거나 토큰이 null이거나 빈문자열일때
            return false;
        }
    }
    // JWT에서 memberId 추출
    public Long getMemberId(String token) {
        return Long.parseLong(
                Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
        );
    }
    public String getRole(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }

    public LocalDateTime getExpiration(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

    }
}
