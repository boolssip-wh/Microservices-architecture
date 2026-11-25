package com.swu.smartgrade.common.api;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String traceId,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>(0, "ok", data, traceId, OffsetDateTime.now());
    }

    public static <T> ApiResponse<T> fail(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId, OffsetDateTime.now());
    }
}
