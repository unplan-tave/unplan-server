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
    KAKAO_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "KAKAO_SERVER_ERROR", "카카오 서버 통신 오류가 발생했습니다"),
    INVALID_KAKAO_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_KAKAO_TOKEN", "유효하지 않은 카카오 액세스 토큰입니다"),
    INVALID_GOOGLE_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "유효하지 않은 구글 ID 토큰입니다"),
    GOOGLE_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "GOOGLE_SERVER_ERROR", "구글 서버와의 통신에 실패하였습니다"),

    // --- [메모 관련 에러] ---
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMO_404", "존재하지 않는 메모가 포함되어 있습니다."),
    MEMO_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "MEMO_COUNT_EXCEEDED", "날짜별 메모는 최대 5개까지만 입력 가능합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}