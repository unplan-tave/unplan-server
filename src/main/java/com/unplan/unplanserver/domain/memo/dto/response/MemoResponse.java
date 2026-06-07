package com.unplan.unplanserver.domain.memo.dto.response;

import com.unplan.unplanserver.domain.memo.entity.Memo;
import lombok.Builder;

@Builder
public record MemoResponse(
        Long dailyMemoId,
        String content
) {
    public static MemoResponse from(Memo dailyMemo) {
        return MemoResponse.builder()
                .dailyMemoId(dailyMemo.getDailyMemoId())
                .content(dailyMemo.getContent())
                .build();
    }
}