package com.unplan.unplanserver.domain.onboarding.entity;

import com.unplan.unplanserver.domain.onboarding.enums.RecoveryMethodType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recover_entity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecoverEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recover_entity_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_method")
    private RecoveryMethodType defaultMethod;

    @Lob
    @Column(name = "custom_method")
    private String customMethod;
}