package com.swu.smartgrade.answer.infrastructure.client;

import com.swu.smartgrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import com.swu.smartgrade.answer.infrastructure.client.fallback.GradingClientFallbackFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "smartgrade-grading-service", fallbackFactory = GradingClientFallbackFactory.class)
public interface GradingClient {
    @org.springframework.web.bind.annotation.GetMapping("/internal/grading/{taskId}")
    ApiResponse<TaskView> get(@PathVariable String taskId);
    @PostMapping("/internal/grading/{taskId}/retry")
    ApiResponse<TaskView> retry(@PathVariable String taskId, @RequestBody RetryRequest request);

    record RetryRequest(String retryReason, String idempotencyKey) { }
    record TaskView(String taskId, String answerId, String status, int retryCount, String errorCode) { }
    @PostMapping("/internal/grading/{taskId}/ocr")
    ApiResponse<OcrResult> ocr(@PathVariable String taskId, @RequestBody OcrRequest request);

    @PostMapping("/internal/grading/{taskId}/match")
    ApiResponse<MatchResult> match(@PathVariable String taskId, @RequestBody MatchRequest request);

    record OcrRequest(String answerId, String objectKey, String text) {
    }

    record OcrResult(
            String text,
            BigDecimal confidence,
            BigDecimal answerCoverageRatio,
            int characterCount,
            String provider,
            String failureReason,
            long durationMs
    ) {
    }

    record MatchRequest(
            String text,
            int rubricVersion,
            List<RubricClient.RemotePoint> points,
            BigDecimal answerCoverageRatio,
            BigDecimal maxScore,
            BigDecimal baseScore
    ) {
        public MatchRequest(String text, int rubricVersion, List<RubricClient.RemotePoint> points,
                            BigDecimal answerCoverageRatio, BigDecimal maxScore) {
            this(text, rubricVersion, points, answerCoverageRatio, maxScore, null);
        }
    }

    record MatchResult(List<Hit> hits, BigDecimal suggestedScore, BigDecimal confidence) {
    }

    record Hit(String pointId, String pointName, String matchType, String evidence,
               int start, int end, BigDecimal confidence) {
    }
}
