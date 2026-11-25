package com.swu.smartgrade.report.infrastructure.client;

import com.swu.smartgrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "smartgrade-review-service")
public interface ReviewClient {
    @GetMapping("/internal/results/batches/{batchId}/summary")
    ApiResponse<RemoteSummary> summary(@PathVariable String batchId);

    record RemoteSummary(
            List<FinalizedScore> finalScores,
            List<PointMissCount> pointMissCounts,
            List<PointMissRate> pointMissRates
    ) {
    }

    record FinalizedScore(String answerId, String studentNo, BigDecimal finalScore, BigDecimal maxScore) {
    }

    record PointMissCount(String pointId, String pointName, long missCount) {
    }

    record PointMissRate(String pointId, String pointName, double missRate) {
    }
}
