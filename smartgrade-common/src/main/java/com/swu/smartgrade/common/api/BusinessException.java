package com.swu.smartgrade.common.api;

public class BusinessException extends RuntimeException {
    private final int code;
    private final int httpStatus;

    public BusinessException(int code, String message) {
        this(code, code / 100, message);
    }

    public BusinessException(int code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public static BusinessException badRequest(String message) {
        return new BusinessException(40000, 400, message);
    }

    public static BusinessException unauthorized(String message) {
        return new BusinessException(40100, 401, message);
    }

    public static BusinessException forbidden(String message) {
        return new BusinessException(40300, 403, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(40400, 404, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(40900, 409, message);
    }

    public static BusinessException tooManyRequests(String message) {
        return new BusinessException(42900, 429, message);
    }

    public static BusinessException unavailable(String message) {
        return new BusinessException(50300, 503, message);
    }
}
