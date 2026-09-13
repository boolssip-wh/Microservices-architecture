package com.swu.smartgrade.common.api;

/**
 * 业务异常：统一携带业务错误码，由 {@link GlobalExceptionHandler} 转成响应体。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    public int getCode() {
        return code;
    }
}
