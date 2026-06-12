package com.unplan.unplanserver.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // --- [공통 에러] ---
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "VALID_400", "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_401", "유효하지 않은 토큰입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_500", "서버 내부 오류가 발생했습니다."),

    // --- [회원 관련 에러] ---
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_404", "존재하지 않는 회원입니다."),

    // --- [메모 관련 에러] ---
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMO_404", "존재하지 않는 메모가 포함되어 있습니다."),
    MEMO_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "MEMO_COUNT_EXCEEDED", "날짜별 메모는 최대 5개까지만 입력 가능합니다."),

    // --- [온보딩 관련 에러] ---
    RECOVER_METHOD_REQUIRED(HttpStatus.BAD_REQUEST, "RECOVER_400", "최소 1개 이상의 회복 방법을 선택해야 합니다."),
    INVALID_SLEEP_RANGE(HttpStatus.BAD_REQUEST, "INVALID_SLEEP_RANGE", "각 수면 구간은 최소 30분 이상, 최대 10시간 30분 이하여야 합니다."),
    INVALID_SLEEP_UNIT(HttpStatus.BAD_REQUEST, "INVALID_SLEEP_UNIT", "수면 시간은 30분 단위로 입력해야 합니다."),
    INVALID_SLEEP_ORDER(HttpStatus.BAD_REQUEST, "INVALID_SLEEP_ORDER", "각 구간의 시간 설정 순서가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}