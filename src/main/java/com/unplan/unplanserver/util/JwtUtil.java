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

    public Claims parseClaims(String token, Boolean isAccess){
        try{
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            if(type == null)    return null;
            if(isAccess && !"access".equals(type))  return null;
            if(!isAccess && !"refresh".equals(type)) return null;

            return claims;
        }catch (JwtException | IllegalArgumentException e){
            throw new ExpiredJwtException(null, null, "만료된 토큰");
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
    public String getRole(Claims claims){
        return claims.get("role", String.class);
    }

    public LocalDateTime getExpiration(Claims claims) {
        return claims
                .getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public LocalDateTime getIssuedAt(Claims claims) {
        return claims
                .getIssuedAt()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }
}
