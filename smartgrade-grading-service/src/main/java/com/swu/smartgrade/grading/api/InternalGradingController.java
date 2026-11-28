package com.swu.smartgrade.grading.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.grading.api.dto.MatchRequest;
import com.swu.smartgrade.grading.api.dto.MatchResult;
import com.swu.smartgrade.grading.api.dto.GradingCapabilities;
import com.swu.smartgrade.grading.api.dto.OcrRequest;
import com.swu.smartgrade.grading.api.dto.OcrResult;
import com.swu.smartgrade.grading.api.dto.RetryRequest;
import com.swu.smartgrade.grading.api.dto.TaskView;
import com.swu.smartgrade.grading.application.GradingApplicationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A4/C3/C04 内部接口：能力查询、OCR/图片识别、要点匹配、任务查询和失败重试。
 * answer-service 通过 Feign 按服务名调用这些接口。
 */
@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "C04 可解释初筛服务")
@RequestMapping("/internal/grading")
public class InternalGradingController {
    private final GradingApplicationService service;

    public InternalGradingController(GradingApplicationService service) {
        this.service = service;
    }

    @PostMapping("/{taskId}/retry")
    public ApiResponse<TaskView> retry(@PathVariable String taskId, @RequestBody RetryRequest request) {
        return ApiResponse.ok(service.retry(taskId, request), TraceId.current());
    }

    @GetMapping("/{taskId}")
    @io.swagger.v3.oas.annotations.Operation(operationId = "C04-4-01", summary = "读取初筛任务状态和失败信息")
    public ApiResponse<TaskView> get(@PathVariable String taskId) {
        return ApiResponse.ok(service.get(taskId), TraceId.current());
    }

    @GetMapping("/capabilities")
    @io.swagger.v3.oas.annotations.Operation(operationId = "C04-1-01", summary = "读取当前初筛适配器能力")
    public ApiResponse<GradingCapabilities> capabilities() {
        return ApiResponse.ok(service.capabilities(), TraceId.current());
    }

    @PostMapping("/{taskId}/ocr")
    @io.swagger.v3.oas.annotations.Operation(operationId = "C04-1-02", summary = "执行 OCR 适配或文本直读")
    public ApiResponse<OcrResult> ocr(@PathVariable String taskId, @RequestBody OcrRequest request) {
        return ApiResponse.ok(service.ocr(taskId, request), TraceId.current());
    }

    @PostMapping("/{taskId}/match")
    @io.swagger.v3.oas.annotations.Operation(operationId = "C04-2-01", summary = "按冻结规则匹配并生成证据和建议分")
    public ApiResponse<MatchResult> match(@PathVariable String taskId, @RequestBody MatchRequest request) {
        return ApiResponse.ok(service.match(taskId, request), TraceId.current());
    }
}
