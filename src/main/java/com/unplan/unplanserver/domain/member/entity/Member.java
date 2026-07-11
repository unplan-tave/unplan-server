package com.unplan.unplanserver.domain.member.entity;

import com.unplan.unplanserver.domain.auth.dto.GoogleUserInfoDto;
import com.unplan.unplanserver.domain.auth.dto.response.KakaoUserInfoResponseDto;
import com.unplan.unplanserver.domain.member.dto.UpdateProfileRequestDto;
import com.unplan.unplanserver.domain.member.enums.Provider;
import com.unplan.unplanserver.domain.member.enums.Role;
import jakarta.persistence.*;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.*;
@Entity
@Table(name = "member")
@Getter
@SQLDelete(sql = "UPDATE member SET deleted_at = NOW() WHERE member_id = ?")
@SQLRestriction("deleted_at IS NULL")
@EntityListeners(AuditingEntityListener.class)
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
    private Role role;

//    @Enumerated(EnumType.STRING)
//    private Gender gender;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(nullable = false)
    private String nickname;

    private String email;

//    private LocalDate birth;

//    @Column(name = "transport_type")
//    @Enumerated(EnumType.STRING)
//    private TransportType transportType;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "onboarding_completed")
    private Boolean onboardingCompleted;

    public static Member fromKakao(KakaoUserInfoResponseDto userInfo){
        Member member = new Member();
        member.oauthId = userInfo.getId().toString();
        member.role = Role.USER;
        member.provider = Provider.KAKAO;
        if (!userInfo.getKakaoAccount().getProfileNicknameNeedsAgreement()) {
            member.nickname = userInfo.getKakaoAccount().getProfile().getNickname();
        }
        if(!userInfo.getKakaoAccount().getEmailNeedsAgreement()){
            member.email = userInfo.getKakaoAccount().getEmail();
        }
        member.onboardingCompleted = false;
        return member;
    }
    public static Member fromGoogle(GoogleUserInfoDto userInfo){
        Member member = new Member();
        member.oauthId = userInfo.getOauthId();
        member.role = Role.USER;
        member.provider = Provider.GOOGLE;
        member.nickname = userInfo.getNickname();
        member.email = userInfo.getEmail();
        member.onboardingCompleted = false;
        return member;
    }

    public void updateProfile(UpdateProfileRequestDto requestDto) {
        String name = requestDto.name();
        if (notBlank(name)) {
            this.name = name;
        }
        String nickname = requestDto.nickname();
        if (notBlank(nickname)) {
            this.nickname = nickname;
        }
        String email = requestDto.email();
        if (notBlank(email)) {
            this.email = email;
        }
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }
    private boolean notBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }
}
