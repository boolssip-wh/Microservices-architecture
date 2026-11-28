package com.swu.smartgrade.answer.infrastructure.client;

import com.swu.smartgrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import com.swu.smartgrade.answer.infrastructure.client.fallback.GradingClientFallbackFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.List;

/**
 * B2/C3 证据：初筛服务远程调用接口。
 * 服务名来自 Nacos 注册表，调用失败由 GradingClientFallbackFactory 转成统一错误响应。
 */
@FeignClient(name = "smartgrade-grading-service", fallbackFactory = GradingClientFallbackFactory.class)
public interface GradingClient {
    // 查询任务状态：供重试流程判断任务当前处于 PENDING、成功还是人工复核。
    @org.springframework.web.bind.annotation.GetMapping("/internal/grading/{taskId}")
    ApiResponse<TaskView> get(@PathVariable String taskId);
    // 显式重试接口：调用端传入幂等键，避免同一失败任务被重复消费。
    @PostMapping("/internal/grading/{taskId}/retry")
    ApiResponse<TaskView> retry(@PathVariable String taskId, @RequestBody RetryRequest request);

    record RetryRequest(String retryReason, String idempotencyKey) { }
    record TaskView(String taskId, String answerId, String status, int retryCount, String errorCode) { }
    // C3 真实远程调用：answer-service 把图片对象地址或文本提交给 grading-service 识别。
    @PostMapping("/internal/grading/{taskId}/ocr")
    ApiResponse<OcrResult> ocr(@PathVariable String taskId, @RequestBody OcrRequest request);

    // C3 真实远程调用：识别完成后把规则和文本提交给评分匹配器。
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
