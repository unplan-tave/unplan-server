package com.unplan.unplanserver.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // --- [공통 에러] ---
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "입력값이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "유효하지 않은 토큰입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // --- [회원 관련 에러] ---
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "존재하지 않는 회원입니다."),
    KAKAO_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "KAKAO_SERVER_ERROR", "카카오 서버 통신 오류가 발생했습니다"),
    INVALID_KAKAO_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_KAKAO_TOKEN", "유효하지 않은 카카오 액세스 토큰입니다"),
    INVALID_GOOGLE_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN", "유효하지 않은 구글 ID 토큰입니다"),
    GOOGLE_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "GOOGLE_SERVER_ERROR", "구글 서버와의 통신에 실패하였습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다"),

    // --- [메모 관련 에러] ---
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMO_NOT_FOUND", "존재하지 않는 메모가 포함되어 있습니다."),
    MEMO_COUNT_EXCEEDED(HttpStatus.BAD_REQUEST, "MEMO_COUNT_EXCEEDED", "날짜별 메모는 최대 5개까지만 입력 가능합니다."),

    // --- [온보딩 관련 에러] ---
    RECOVER_METHOD_REQUIRED(HttpStatus.BAD_REQUEST, "RECOVER_METHOD_REQUIRED", "최소 1개 이상의 회복 방법을 선택해야 합니다."),
    INVALID_SLEEP_RANGE(HttpStatus.BAD_REQUEST, "INVALID_SLEEP_RANGE", "각 수면 구간은 최소 30분 이상, 최대 10시간 30분 이하여야 합니다."),
    INVALID_SLEEP_UNIT(HttpStatus.BAD_REQUEST, "INVALID_SLEEP_UNIT", "수면 시간은 30분 단위로 입력해야 합니다."),
    INVALID_SLEEP_ORDER(HttpStatus.BAD_REQUEST, "INVALID_SLEEP_ORDER", "각 구간의 시간 설정 순서가 올바르지 않습니다."),
    SLEEP_CONDITION_NOT_FOUND(HttpStatus.NOT_FOUND, "SLEEP_CONDITION_NOT_FOUND","수면 컨디션 설정을 찾을 수 없습니다."),
    SLEEP_REQUIRED(HttpStatus.BAD_REQUEST, "SLEEP_REQUIRED", "수면 시간은 최소 한 칸 이상 필수 입력해야 합니다."),
    INVALID_SLEEP_PATTERN(HttpStatus.BAD_REQUEST, "INVALID_SLEEP_PATTERN", "수면 시간은 중간에 끊어서 입력할 수 없습니다."),

    // --- [일정 관련 에러] ---
    SCHEDULE_NOT_FOUND(HttpStatus.NOT_FOUND, "SCHEDULE_NOT_FOUND", "존재하지 않는 일정입니다."),
    INVALID_MONTH_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_MONTH_FORMAT", "월 형식이 올바르지 않습니다. (yyyy-MM)"),
    INVALID_RECURRENCE(HttpStatus.BAD_REQUEST, "INVALID_RECURRENCE", "반복 설정 형식이 올바르지 않습니다. by_month_day는 1~31 숫자, by_day는 요일(MON,TUE...) 또는 N번째 요일(2WED) 형식이어야 합니다."),
    AI_API_UNAVAILABLE(HttpStatus.INTERNAL_SERVER_ERROR, "AI_API_UNAVAILABLE", "AI 서비스에 연결할 수 없습니다."),
    AI_RESPONSE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI_RESPONSE_PARSE_ERROR", "AI 응답을 처리할 수 없습니다."),

    INVALID_SCHEDULE_TIME(HttpStatus.BAD_REQUEST, "INVALID_SCHEDULE_TIME", "일정 시간이 올바르지 않습니다. 시작/종료 시간은 함께 입력해야 하며, 시작 시간은 종료 시간보다 이전이어야 합니다."),
    NOT_A_QUEUE_CARD(HttpStatus.BAD_REQUEST, "NOT_A_QUEUE_CARD", "큐 카드가 아닌 일정에는 추천 시간대를 제공할 수 없습니다."),
    PERSONAL_TAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "PERSONAL_TAG_LIMIT_EXCEEDED", "개인 태그는 계정당 최대 100개까지 생성할 수 있습니다."),

    // --- [컨디션 관련 에러] ---
    CONDITION_NOT_FOUND(HttpStatus.NOT_FOUND, "CONDITION_NOT_FOUND", "해당 조건을 만족하는 컨디션 정보를 찾을 수 없습니다."),
    SLEEP_NOT_FOUND(HttpStatus.NOT_FOUND, "SLEEP_NOT_FOUND", "존재하지 않는 수면 기록입니다."),
    SLEEP_TIME_OVERLAP(HttpStatus.BAD_REQUEST, "SLEEP_TIME_OVERLAP", "수면 시간대와 컨디션 시간이 겹칩니다."),
    CONDITION_TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "CONDITION_TAG_NOT_FOUND", "컨디션 태그를 찾을 수 없습니다."),

    // --- [추천 관련 에러] ---
    RECOMMENDATION_NOT_FOUND(HttpStatus.NOT_FOUND, "RECOMMENDATION_404", "존재하지 않는 추천입니다."),
    RECOMMENDATION_ALREADY_PROCESSED(HttpStatus.CONFLICT, "RECOMMENDATION_409", "이미 수락된 추천입니다."),
    RECOVERY_MEAN_INVALID(HttpStatus.BAD_REQUEST, "RECOVERY_MEAN_400", "선택한 회복 수단이 올바르지 않습니다."),

    // --- [설정 관련 에러] ---
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "INVALID_TIME_RANGE", "종료시간이 시작시간보다 빠릅니다."),
    TIME_RANGE_OVERLAP(HttpStatus.CONFLICT, "TIME_RANGE_OVERLAP", "입력한 빈시간 추천 제외 시간대가 기존의 빈시간 추천 제외 시간대와 겹칩니다."),
    SETTING_NOT_FOUND(HttpStatus.NOT_FOUND, "SETTING_NOT_FOUND", "설정을 찾을 수 없습니다"),
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
