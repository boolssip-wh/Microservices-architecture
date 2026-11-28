package com.swu.smartgrade.answer.api;

import com.swu.smartgrade.answer.api.dto.AnswerView;
import com.swu.smartgrade.answer.api.dto.BatchDetailView;
import com.swu.smartgrade.answer.api.dto.BatchView;
import com.swu.smartgrade.answer.api.dto.CreateBatchRequest;
import com.swu.smartgrade.answer.api.dto.ImportResult;
import com.swu.smartgrade.answer.api.dto.ProgressView;
import com.swu.smartgrade.answer.application.AnswerImportApplicationService;
import com.swu.smartgrade.answer.application.BatchApplicationService;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.PageResult;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A4/C03 接口入口：创建批次、查进度、导入答卷和分页查看答卷。
 * Controller 只做参数接收和 ApiResponse 包装，业务规则在 application 层。
 */
@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name="C03 答卷批次服务")
@RequestMapping("/api/batches")
public class BatchController {
    private final BatchApplicationService service;
    private final AnswerImportApplicationService importService;

    public BatchController(BatchApplicationService service, AnswerImportApplicationService importService) {
        this.service = service;
        this.importService = importService;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(operationId="C03-1-02", summary="分页查看本人阅卷批次")
    public ApiResponse<PageResult<BatchView>> list(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(AuthPrincipal.required().userId(), status, page, size), TraceId.current());
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(operationId="C03-1-01", summary="3 步内创建阅卷批次")
    public ApiResponse<BatchView> create(@Valid @RequestBody CreateBatchRequest request) {
        return ApiResponse.ok(service.create(AuthPrincipal.required().userId(), request), TraceId.current());
    }

    @GetMapping("/{batchId}")
    @io.swagger.v3.oas.annotations.Operation(operationId="C03-1-03", summary="读取批次详情与当前状态")
    public ApiResponse<BatchDetailView> detail(@PathVariable String batchId) {
        return ApiResponse.ok(service.detail(AuthPrincipal.required().userId(), batchId), TraceId.current());
    }

    @GetMapping("/{batchId}/progress")
    @io.swagger.v3.oas.annotations.Operation(operationId="C03-4-01", summary="1 个 jobId 跟踪全批次进度")
    public ApiResponse<ProgressView> progress(@PathVariable String batchId) {
        service.requireOwned(AuthPrincipal.required().userId(), batchId);
        return ApiResponse.ok(service.progress(batchId), TraceId.current());
    }

    @PostMapping("/{batchId}/answers/import")
    @io.swagger.v3.oas.annotations.Operation(operationId="C03-2-01", summary="单次导入 20 份答卷")
    public ApiResponse<ImportResult> importAnswers(
            @PathVariable String batchId,
            @RequestParam("files") List<MultipartFile> files,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestParam String sourceType,
            @RequestParam(defaultValue = "filename") String numberingRule) {
        return ApiResponse.ok(importService.importAnswers(AuthPrincipal.required().userId(), batchId,
                files, sourceType, numberingRule, idempotencyKey), TraceId.current());
    }

    @GetMapping("/{batchId}/answers")
    @io.swagger.v3.oas.annotations.Operation(operationId="C03-4-02", summary="读取批次答卷和处理状态")
    public ApiResponse<PageResult<AnswerView>> answers(@PathVariable String batchId,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size,
                                                       @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.answers(AuthPrincipal.required().userId(), batchId, status, page, size),
                TraceId.current());
    }
}
