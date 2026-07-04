package com.unplan.unplanserver.domain.memo.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

public class MemoRequest {

    // 메모 작성
    @Builder
    public record Create(
            @NotNull
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
            LocalDate date,

            @NotBlank(message = "메모 내용은 비어있을 수 없습니다.")
            @Size(min = 1, max = 20, message = "메모는 1자 이상 20자 이하로 입력해주세요.")
            String content
    ) {}

    // 메모 삭제
    @Builder
    public record Delete(
            @NotEmpty(message = "삭제할 메모 ID를 1개 이상 선택해주세요.")
            List<Long> dailyMemoIds
    ) {}
}