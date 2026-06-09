package com.unplan.unplanserver.domain.memo.service;

import com.unplan.unplanserver.domain.memo.dto.request.MemoRequest;
import com.unplan.unplanserver.domain.memo.dto.response.MemoResponse;
import com.unplan.unplanserver.domain.memo.entity.Memo;
import com.unplan.unplanserver.domain.memo.repository.MemoRepository;
import com.unplan.unplanserver.global.exception.CustomException;
import com.unplan.unplanserver.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;

    // 메모 생성
    @Transactional
    public MemoResponse createMemo(Long memberId, MemoRequest.Create request) {
        int currentMemoCount = memoRepository.countByMemberIdAndDate(memberId, request.date());
        if (currentMemoCount >= 5) {
            throw new CustomException(ErrorCode.MEMO_COUNT_EXCEEDED);
        }

        Memo memo = Memo.builder()
                .memberId(memberId)
                .date(request.date())
                .content(request.content())
                .build();

        Memo savedMemo = memoRepository.save(memo);
        return MemoResponse.from(savedMemo);
    }

    // 메모 조회
    @Transactional(readOnly = true)
    public List<MemoResponse> getMemos(Long memberId, LocalDate date) {
        List<Memo> memos = memoRepository.findByMemberIdAndDate(memberId, date);

        return memos.stream()
                .map(MemoResponse::from)
                .toList();
    }

    // 메모 삭제
    @Transactional
    public void deleteMemos(Long memberId, MemoRequest.Delete request) {

        List<Long> uniqueRequestedIds = request.dailyMemoIds().stream()
                .distinct()
                .toList();

        List<Memo> existingMemos = memoRepository.findAllByDailyMemoIdInAndMemberId(uniqueRequestedIds, memberId);

        if (existingMemos.size() != uniqueRequestedIds.size()) {
            throw new CustomException(ErrorCode.MEMO_NOT_FOUND);
        }

        memoRepository.deleteAllInBatch(existingMemos);
    }
}