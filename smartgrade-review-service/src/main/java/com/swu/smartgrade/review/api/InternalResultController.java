package com.swu.smartgrade.review.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.review.api.dto.CreateResultRequest;
import com.swu.smartgrade.review.api.dto.CreatedResult;
import com.swu.smartgrade.review.api.dto.ResultSummary;
import com.swu.smartgrade.review.application.ReviewApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A4/D1 内部接口：接收 answer-service 投递的复核投影和汇总数据。
 * 这是 Seata 全局事务涉及的远程写入入口。
 */
@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "C05 复核定分服务")
@RequestMapping("/internal/results")
public class InternalResultController {
    private final ReviewApplicationService service;

    public InternalResultController(ReviewApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(operationId = "C05-1-01", summary = "接收初筛结果并创建待复核记录")
    public ApiResponse<CreatedResult> create(@RequestBody CreateResultRequest request) {
        return ApiResponse.ok(service.create(request), TraceId.current());
    }

    @GetMapping("/batches/{batchId}/summary")
    @io.swagger.v3.oas.annotations.Operation(operationId = "C05-3-02", summary = "供 C06 读取已定分结果汇总")
    public ApiResponse<ResultSummary> summary(@PathVariable String batchId) {
        return ApiResponse.ok(service.ownedSummary(AuthPrincipal.required().userId(), batchId), TraceId.current());
    }

    @GetMapping("/batches/{batchId}/states")
    public ApiResponse<java.util.List<ReviewApplicationService.ResultState>> states(@PathVariable String batchId) {
        return ApiResponse.ok(service.states(AuthPrincipal.required().userId(), batchId), TraceId.current());
    }
}
