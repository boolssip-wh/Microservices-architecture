package com.swu.smartgrade.review.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.PageResult;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.review.api.dto.*;
import com.swu.smartgrade.review.application.ReviewApplicationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name = "C05 复核定分服务")
@RequestMapping("/api/results")
public class ResultController {
    private final ReviewApplicationService service;

    public ResultController(ReviewApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(operationId = "C05-1-02", summary = "分页筛选本人复核结果")
    public ApiResponse<PageResult<ResultListItem>> list(@RequestParam(required = false) String batchId,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(defaultValue = "1") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(AuthPrincipal.required().userId(), batchId, status, page, size),
                TraceId.current());
    }

    @GetMapping("/{answerId}")
    @io.swagger.v3.oas.annotations.Operation(operationId = "C05-2-01", summary = "读取单份答卷证据化复核详情")
    public ApiResponse<ResultDetail> detail(@PathVariable String answerId) {
        return ApiResponse.ok(service.detail(AuthPrincipal.required().userId(), answerId), TraceId.current());
    }

    @PostMapping("/{answerId}/comment-suggestion")
    @io.swagger.v3.oas.annotations.Operation(operationId = "C05-4-01", summary = "生成两条可编辑评语草稿")
    public ApiResponse<CommentSuggestionResponse> suggestComment(
            @PathVariable String answerId,
            @Valid @RequestBody CommentSuggestionRequest request) {
        return ApiResponse.ok(service.suggestComment(AuthPrincipal.required().userId(), answerId, request),
                TraceId.current());
    }

    @PutMapping("/{answerId}/final-score")
    @io.swagger.v3.oas.annotations.Operation(operationId = "C05-3-01", summary = "确认最终分 最终评语并写入快照和审计")
    public ApiResponse<FinalScoreResponse> finalizeScore(
            @PathVariable String answerId,
            @Valid @RequestBody FinalScoreRequest request) {
        return ApiResponse.ok(service.finalizeScore(AuthPrincipal.required().userId(), answerId, request),
                TraceId.current());
    }

    @PutMapping("/{answerId}/review-status")
    @io.swagger.v3.oas.annotations.Operation(operationId = "C05-2-02", summary = "标记待人工 存疑或已复核状态")
    public ApiResponse<ReviewStatusResponse> updateStatus(
            @PathVariable String answerId,
            @Valid @RequestBody ReviewStatusRequest request) {
        return ApiResponse.ok(service.updateStatus(AuthPrincipal.required().userId(), answerId, request),
                TraceId.current());
    }
}
