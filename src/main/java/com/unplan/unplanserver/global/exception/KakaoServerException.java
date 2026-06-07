package com.unplan.unplanserver.global.exception;

public class KakaoServerException extends RuntimeException{
    public KakaoServerException() {
        super("카카오 서버 오류");
    }
}
