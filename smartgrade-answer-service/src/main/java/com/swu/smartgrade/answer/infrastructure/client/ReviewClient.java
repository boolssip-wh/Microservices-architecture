package com.swu.smartgrade.answer.infrastructure.client;

import com.swu.smartgrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "smartgrade-review-service")
public interface ReviewClient {
    @org.springframework.web.bind.annotation.GetMapping("/internal/results/batches/{batchId}/states")
    ApiResponse<List<ReviewState>> states(@org.springframework.web.bind.annotation.PathVariable String batchId);
    record ReviewState(String answerId, String status) { }
    @PostMapping("/internal/results")
    ApiResponse<CreatedResult> create(@RequestBody CreateResultRequest request);

    record CreateResultRequest(
            String answerId,
            String batchId,
            String studentNo,
            String answerText,
            Long teacherId,
            int rubricVersion,
            BigDecimal suggestedScore,
            BigDecimal maxScore,
            BigDecimal answerCoverageRatio,
            BigDecimal confidence,
            List<GradingClient.Hit> hits,
            String status,
            List<ScoringPoint> scoringPoints,
            BigDecimal baseScore
    ) {
        public CreateResultRequest(String answerId, String batchId, String studentNo, String answerText,
                                   Long teacherId, int rubricVersion, BigDecimal suggestedScore,
                                   BigDecimal maxScore, BigDecimal answerCoverageRatio, BigDecimal confidence,
                                   List<GradingClient.Hit> hits, String status, List<ScoringPoint> scoringPoints) {
            this(answerId, batchId, studentNo, answerText, teacherId, rubricVersion, suggestedScore,
                    maxScore, answerCoverageRatio, confidence, hits, status, scoringPoints, null);
        }
    }

    record ScoringPoint(String pointId, String pointName) { }

    record CreatedResult(String resultId, String status) {
    }
}
