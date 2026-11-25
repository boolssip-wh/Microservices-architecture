package com.swu.smartgrade.answer.infrastructure.client.fallback;

import com.swu.smartgrade.answer.infrastructure.client.RubricClient;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RubricClientFallbackFactory implements FallbackFactory<RubricClient> {
    @Override
    public RubricClient create(Throwable cause) {
        String reason = "评分规则服务已熔断或不可用：" + rootMessage(cause);
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
