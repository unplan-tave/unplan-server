package com.unplan.unplanserver.domain.measurement.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SleepGetApiResponse {

    private static final String SUCCESS_CODE = "COMMON_200";
    private static final String SUCCESS_MESSAGE = "수면 기록 조회가 완료되었습니다.";

    private final boolean success;
    private final String code;
    private final String message;
    private final LocalDateTime timestamp;
    private final SleepResponse data;

    private SleepGetApiResponse(boolean success, String code, String message, SleepResponse data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    public static SleepGetApiResponse success(SleepResponse data) {
        return new SleepGetApiResponse(true, SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    public static SleepGetApiResponse fail(String code, String message) {
        return new SleepGetApiResponse(false, code, message, null);
    }
}
