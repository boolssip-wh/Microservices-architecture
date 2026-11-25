package com.swu.smartgrade.answer.api;

import com.swu.smartgrade.answer.application.GradingOrchestrator;
import com.swu.smartgrade.answer.application.ImportReadyEvent;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/import-jobs")
@Tag(name = "C03 答卷批次服务")
public class InternalImportJobController {
    private final GradingOrchestrator orchestrator;
    public InternalImportJobController(GradingOrchestrator orchestrator) { this.orchestrator = orchestrator; }

    @PostMapping("/{jobId}/process")
    @Operation(summary = "供初筛事件消费者幂等推进导入批次")
    public ApiResponse<String> process(@PathVariable String jobId) {
        orchestrator.process(new ImportReadyEvent(jobId));
        return ApiResponse.ok(jobId, TraceId.current());
    }
}
