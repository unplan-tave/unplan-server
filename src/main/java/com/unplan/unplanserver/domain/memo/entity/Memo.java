package com.unplan.unplanserver.domain.memo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "daily_memo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Memo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_memo_id")
    private Long dailyMemoId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "memo_date", nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 20)
    private String content;
}