package com.cosync.wallet.common;

import java.util.Map;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final Map<String, String> details;

    public ApiException(HttpStatus status, String code, String message) {
        this(status, code, message, null);
    }

    /** details = ข้อมูลประกอบ error เช่น INSUFFICIENT_BALANCE ส่ง balance กับ shortfall */
    public ApiException(HttpStatus status, String code, String message, Map<String, String> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public Map<String, String> getDetails() { return details; }
}
