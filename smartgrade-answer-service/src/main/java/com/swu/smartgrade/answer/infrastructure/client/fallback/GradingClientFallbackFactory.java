package com.swu.smartgrade.answer.infrastructure.client.fallback;

import com.swu.smartgrade.answer.infrastructure.client.GradingClient;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * C3 降级证据：grading-service 超时、熔断或不可用时，调用端仍能得到结构化响应。
 * 查询、重试、匹配统一返回 50301；OCR 结果保留 failureReason，交由编排器置为 FAILED。
 */
@Component
public class GradingClientFallbackFactory implements FallbackFactory<GradingClient> {
    @Override
    public GradingClient create(Throwable cause) {
        // 根因信息用于现场说明超时/连接失败的具体来源。
        String reason = "评分服务已熔断或不可用：" + rootMessage(cause);
        return new GradingClient() {
            @Override
            public ApiResponse<GradingClient.TaskView> get(String taskId) {
                return ApiResponse.fail(50301, reason, TraceId.current());
            }
            @Override
            public ApiResponse<GradingClient.TaskView> retry(String taskId, GradingClient.RetryRequest request) {
                return ApiResponse.fail(50301, reason, TraceId.current());
            }
            @Override
            public ApiResponse<GradingClient.OcrResult> ocr(String taskId, GradingClient.OcrRequest request) {
                // OCR 适配统一结果结构，并明确写入 failureReason；调用端据此走失败处理。
                return ApiResponse.ok(new GradingClient.OcrResult(
                        null, BigDecimal.ZERO, BigDecimal.ZERO, 0, "fallback",
                        reason, 0), TraceId.current());
            }

            @Override
            public ApiResponse<GradingClient.MatchResult> match(String taskId,
                                                                 GradingClient.MatchRequest request) {
                return ApiResponse.fail(50301, reason, TraceId.current());
            }
        };
    }

    private String rootMessage(Throwable cause) {
        Throwable current = cause;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null ? "unknown" : current.getMessage();
    }
}
