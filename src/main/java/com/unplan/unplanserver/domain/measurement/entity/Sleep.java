package com.unplan.unplanserver.domain.measurement.entity;

import com.unplan.unplanserver.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "sleeps",
        indexes = {
                @Index(name = "idx_sleeps_member_id", columnList = "member_id"),
                @Index(name = "idx_sleeps_created_at", columnList = "created_at")
        }
)
public class Sleep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sleepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(name = "bed_time", nullable = false)
    private LocalDateTime bedTime;

    @Column(name = "wake_up_time", nullable = false)
    private LocalDateTime wakeUpTime;

    @Column(name = "is_nap", nullable = false)
    private Boolean nap;

    @Column(name = "is_all_night", nullable = false)
    private Boolean allNight;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Sleep(
            Member member,
            Integer durationMinutes,
            LocalDateTime bedTime,
            LocalDateTime wakeUpTime,
            Boolean nap
    ) {
        this(member, durationMinutes, bedTime, wakeUpTime, nap, false);
    }

    public Sleep(
            Member member,
            Integer durationMinutes,
            LocalDateTime bedTime,
            LocalDateTime wakeUpTime,
            Boolean nap,
            Boolean allNight
    ) {
        this.member = member;
        this.durationMinutes = durationMinutes;
        this.bedTime = bedTime;
        this.wakeUpTime = wakeUpTime;
        this.nap = nap;
        this.allNight = allNight;
    }

    public void updateSleep(
            Integer durationMinutes,
            LocalDateTime bedTime,
            LocalDateTime wakeUpTime,
            Boolean nap,
            Boolean allNight
    ) {
        this.durationMinutes = durationMinutes;
        this.bedTime = bedTime;
        this.wakeUpTime = wakeUpTime;
        this.nap = nap;
        this.allNight = allNight;
    }
}
