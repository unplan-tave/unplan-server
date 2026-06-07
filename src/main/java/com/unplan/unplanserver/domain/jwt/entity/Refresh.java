package com.unplan.unplanserver.domain.jwt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh")
@AllArgsConstructor
@NoArgsConstructor
public class Refresh {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long refreshId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", updatable = false)
    private LocalDateTime expiresAt;

    public static Refresh of(Long memberId, String token, String deviceId, LocalDateTime expiresAt) {
        Refresh refresh = new Refresh();
        refresh.memberId = memberId;
        refresh.token = token;
        refresh.deviceId = deviceId;
        refresh.expiresAt = expiresAt;
        return refresh;
    }
}
