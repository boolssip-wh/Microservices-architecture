package com.swu.smartgrade.rubric.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.rubric.api.dto.RubricVersionView;
import com.swu.smartgrade.rubric.application.RubricApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A4/B2 内部接口：供 answer-service 按 batchId 读取当前冻结规则。
 */
@RestController
@RequestMapping("/internal/rubrics")
@Tag(name = "C02 评分规则服务")
public class InternalRubricController {
    private final RubricApplicationService service;

    public InternalRubricController(RubricApplicationService service) {
        this.service = service;
    }

    @GetMapping("/batches/{batchId}/current")
    @Operation(operationId = "C02-4-03", summary = "读取批次当前冻结规则")
    public ApiResponse<RubricVersionView> current(@PathVariable String batchId) {
        return ApiResponse.ok(service.currentForBatch(batchId), TraceId.current());
    }
}
