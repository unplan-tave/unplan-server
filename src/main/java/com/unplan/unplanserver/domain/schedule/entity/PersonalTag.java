package com.unplan.unplanserver.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "personal_tag",
        // 같은 멤버 안에서 같은 이름의 태그가 중복 생성되지 않도록 보장 (find-or-create 멱등성)
        uniqueConstraints = @UniqueConstraint(name = "uk_personal_tag_member_name", columnNames = {"member_id", "name"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PersonalTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "personal_tag_id")
    private Long personalTagId;

    // Schedule과 동일하게 Member 연관관계 대신 임시 Long 사용. 추후 @ManyToOne으로 교체 예정
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;
}
