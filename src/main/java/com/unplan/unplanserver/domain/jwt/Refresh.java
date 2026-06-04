package com.unplan.unplanserver.domain.jwt;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh")
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
}
