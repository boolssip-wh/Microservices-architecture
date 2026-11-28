package com.swu.smartgrade.rubric.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.rubric.api.dto.CompileRubricRequest;
import com.swu.smartgrade.rubric.api.dto.CompileRubricResponse;
import com.swu.smartgrade.rubric.api.dto.CreateRubricRequest;
import com.swu.smartgrade.rubric.api.dto.CreateRubricResponse;
import com.swu.smartgrade.rubric.api.dto.ParseRubricRequest;
import com.swu.smartgrade.rubric.api.dto.ParseRubricResponse;
import com.swu.smartgrade.rubric.api.dto.RubricVersionView;
import com.swu.smartgrade.rubric.api.dto.UpdatePointsRequest;
import com.swu.smartgrade.rubric.api.dto.UpdatePointsResponse;
import com.swu.smartgrade.rubric.application.RubricApplicationService;
import com.swu.smartgrade.rubric.application.RubricAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;

/**
 * A4/C02 接口入口：创建、解析、维护、冻结和读取评分规则版本。
 */
@RestController
@Validated
@RequestMapping("/api/rubrics")
@Tag(name = "C02 评分规则服务")
public class RubricController {
    private final RubricApplicationService service;
    private final RubricAccessService access;

    public RubricController(RubricApplicationService service, RubricAccessService access) {
        this.service = service;
        this.access = access;
    }

    @PostMapping
    @Operation(operationId = "C02-1-01", summary = "1 次录入创建评分规则")
    public ApiResponse<CreateRubricResponse> create(@Valid @RequestBody CreateRubricRequest request) {
        return ApiResponse.ok(service.create(access.requireCreate(request)), TraceId.current());
    }

    @PostMapping("/{rubricId}/parse")
    @Operation(operationId = "C02-2-01", summary = "6 类内容拆分评分要点")
    public ApiResponse<ParseRubricResponse> parse(@PathVariable String rubricId,
                                                  @Valid @RequestBody ParseRubricRequest request) {
        return ApiResponse.ok(service.parse(access.requireRubric(rubricId), request.sourceText()), TraceId.current());
    }

    @PutMapping("/{rubricId}/points")
    @Operation(operationId = "C02-3-01", summary = "100% 权重合计自动校验")
    public ApiResponse<UpdatePointsResponse> savePoints(@PathVariable String rubricId,
                                                        @Valid @RequestBody UpdatePointsRequest request) {
        return ApiResponse.ok(service.savePoints(access.requireRubric(rubricId), request.points()), TraceId.current());
    }

    @PostMapping("/{rubricId}/compile")
    @Operation(operationId = "C02-4-01", summary = "1 次冻结形成不可变版本")
    public ApiResponse<CompileRubricResponse> compile(@PathVariable String rubricId,
                                                      @Valid @RequestBody CompileRubricRequest request) {
        return ApiResponse.ok(service.compile(access.requireRubric(rubricId), request.expectedWeightTotal()), TraceId.current());
    }

    @GetMapping("/{rubricId}/versions/{version}")
    @Operation(operationId = "C02-4-02", summary = "读取不可变规则快照")
    public ApiResponse<RubricVersionView> version(@PathVariable String rubricId, @PathVariable @Min(1) int version) {
        return ApiResponse.ok(service.version(access.requireRubric(rubricId), version), TraceId.current());
    }

    @GetMapping("/batches/{batchId}/current")
    public ApiResponse<RubricVersionView> currentForBatch(@PathVariable String batchId) {
        return ApiResponse.ok(service.currentForBatch(access.requireBatch(batchId)), TraceId.current());
    }
}
