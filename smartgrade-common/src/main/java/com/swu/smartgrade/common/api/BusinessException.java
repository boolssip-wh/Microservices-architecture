package com.swu.smartgrade.common.api;

/**
 * B1 统一业务异常：业务层只表达错误类型，HTTP 状态和统一响应由 GlobalExceptionHandler 生成。
 * 例如 40000 表示参数错误、40100 表示未认证、40400 表示资源不存在。
 */
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
        // 参数格式、必填项或业务输入不合法时使用。
        return new BusinessException(40000, 400, message);
    }

    public static BusinessException unauthorized(String message) {
        // B1/C1 证据：未登录、令牌无效或绕过网关时统一返回 40100。
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
