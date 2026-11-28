package com.swu.smartgrade.common.api;

import java.time.OffsetDateTime;

/**
 * B1 统一响应契约：所有业务接口使用同一组 code/message/data/traceId/timestamp 字段。
 * 前端只需按一种结构处理成功和失败；traceId 可与 SkyWalking 和日志关联。
 */
public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String traceId,
        OffsetDateTime timestamp
) {
    /** 成功约定：code=0，message=ok，data 为业务数据。 */
    public static <T> ApiResponse<T> ok(T data, String traceId) {
        return new ApiResponse<>(0, "ok", data, traceId, OffsetDateTime.now());
    }

    /** 失败约定：保留统一错误码和 traceId，data 为空。 */
    public static <T> ApiResponse<T> fail(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId, OffsetDateTime.now());
    }
}
