package com.swu.smartgrade.answer.infrastructure.client;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.answer.infrastructure.client.fallback.RubricClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;

/**
 * B2/C3 证据：评分规则远程调用接口。
 * name 只写 Nacos 服务名，不写 IP；fallbackFactory 保证下游超时或熔断时返回明确失败结果。
 */
@FeignClient(name = "smartgrade-rubric-service", fallbackFactory = RubricClientFallbackFactory.class)
public interface RubricClient {
    // 这里声明真实的 HTTP 契约；运行时由 OpenFeign + Nacos + LoadBalancer 找到 rubric-service 实例。
    @GetMapping("/internal/rubrics/batches/{batchId}/current")
    ApiResponse<RemoteRubric> current(@PathVariable String batchId);

    record RemoteRubric(String rubricId, String batchId, int rubricVersion, List<RemotePoint> points,
                        BigDecimal baseScore, BigDecimal maxScore) {
        public RemoteRubric(String rubricId, String batchId, int rubricVersion, List<RemotePoint> points) {
            this(rubricId, batchId, rubricVersion, points, null, null);
        }
    }

    record RemotePoint(
            String pointId,
            String pointName,
            String pointType,
            List<String> keywords,
            List<String> variants,
            BigDecimal weight,
            BigDecimal maxScore
    ) {
    }
}
