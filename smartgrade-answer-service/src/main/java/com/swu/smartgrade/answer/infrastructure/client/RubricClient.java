package com.swu.smartgrade.answer.infrastructure.client;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.answer.infrastructure.client.fallback.RubricClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "smartgrade-rubric-service", fallbackFactory = RubricClientFallbackFactory.class)
public interface RubricClient {
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
