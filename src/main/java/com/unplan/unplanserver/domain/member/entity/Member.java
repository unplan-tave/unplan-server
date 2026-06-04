package com.unplan.unplanserver.domain.member.entity;

import com.unplan.unplanserver.domain.member.enums.Provider;
import com.unplan.unplanserver.domain.member.enums.TransportType;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.annotation.*;

import java.time.*;
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    private String name;

    @Column(name = "oauth_id")
    private String oauthId;

    @Enumerated(EnumType.STRING)
    private String gender;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(nullable = false)
    private String nickname;

    private String email;

    private LocalDate birth;

    @Column(name = "target_sleep_time")
    private int targetSleepTime;    // 분단위로 저장?

    @Column(name = "transport_type")
    @Enumerated(EnumType.STRING)
    private TransportType transportType;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
