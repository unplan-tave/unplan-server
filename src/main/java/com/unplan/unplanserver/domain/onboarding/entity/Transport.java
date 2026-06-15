package com.unplan.unplanserver.domain.onboarding.entity;

import com.unplan.unplanserver.domain.onboarding.enums.TransportType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "transport_entity",
        indexes = @Index(name = "idx_transport_member_id", columnList = "member_id")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Transport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transport_entity_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false)
    private TransportType transportType;
}