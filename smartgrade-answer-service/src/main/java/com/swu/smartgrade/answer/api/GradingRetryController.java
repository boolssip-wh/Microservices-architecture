package com.swu.smartgrade.answer.api;

import com.swu.smartgrade.answer.application.GradingOrchestrator;
import com.swu.smartgrade.answer.infrastructure.client.GradingClient;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

/**
 * A4/C3 接口入口：教师对 FAILED/MANUAL_REVIEW 任务发起幂等重试。
 * Idempotency-Key 和重试原因由 DTO 校验，实际重试编排在 GradingOrchestrator。
 */
@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name="C04 可解释初筛服务")
@RequestMapping("/api/grading-tasks")
public class GradingRetryController {
    private final GradingOrchestrator service;
    public GradingRetryController(GradingOrchestrator service) { this.service = service; }
    @PostMapping("/{taskId}/retry")
    @io.swagger.v3.oas.annotations.Operation(operationId="C04-4-02", summary="提交一次幂等重试")
    public ApiResponse<GradingClient.TaskView> retry(@PathVariable String taskId,
            @RequestHeader("Idempotency-Key") String key, @Valid @RequestBody Request request) {
        return ApiResponse.ok(service.retry(AuthPrincipal.required().userId(), taskId, request.retryReason(), key), TraceId.current());
    }
    public record Request(@NotBlank @Size(max=200) String retryReason) { }
}
