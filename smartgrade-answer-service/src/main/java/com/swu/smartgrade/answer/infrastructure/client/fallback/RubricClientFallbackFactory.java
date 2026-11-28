package com.swu.smartgrade.answer.infrastructure.client.fallback;

import com.swu.smartgrade.answer.infrastructure.client.RubricClient;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * C3 降级证据：rubric-service 不可用时仍返回 ApiResponse，而不是让异常无边界传播。
 * 50302 是调用方可以直接识别的“评分规则服务不可用”业务错误码。
 */
@Component
public class RubricClientFallbackFactory implements FallbackFactory<RubricClient> {
    @Override
    public RubricClient create(Throwable cause) {
        // 保留最底层根因，现场排查时不会只看到 Feign 外层包装异常。
        String reason = "评分规则服务已熔断或不可用：" + rootMessage(cause);
        // 明确失败响应：code=50302，data=null，traceId 用于和 SkyWalking 日志串联。
        return batchId -> ApiResponse.fail(50302, reason, TraceId.current());
    }

    private String rootMessage(Throwable cause) {
        Throwable current = cause;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null ? "unknown" : current.getMessage();
    }
}
