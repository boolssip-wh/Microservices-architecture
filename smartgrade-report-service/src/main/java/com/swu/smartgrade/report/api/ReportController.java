package com.swu.smartgrade.report.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.report.api.dto.CreateExportRequest;
import com.swu.smartgrade.report.api.dto.ExportCreated;
import com.swu.smartgrade.report.api.dto.ExportView;
import com.swu.smartgrade.report.api.dto.InsightsView;
import com.swu.smartgrade.report.application.ReportApplicationService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A4/C06 接口入口：批次统计洞察、异步导出和导出结果查询。
 */
@RestController
@Tag(name = "C06 统计报告服务")
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportApplicationService service;

    public ReportController(ReportApplicationService service) {
        this.service = service;
    }

    @PostMapping("/exports")
    @Operation(operationId = "C06-2-01", summary = "创建 CSV 或 XLSX 成绩导出任务")
    public ApiResponse<ExportCreated> create(@Valid @RequestBody CreateExportRequest request) {
        return ApiResponse.ok(service.create(AuthPrincipal.required().userId(), request), TraceId.current());
    }

    @GetMapping("/exports/{exportId}")
    @Operation(operationId = "C06-2-02", summary = "查询导出状态和下载地址")
    public ApiResponse<ExportView> get(@PathVariable String exportId) {
        return ApiResponse.ok(service.get(AuthPrincipal.required().userId(), exportId), TraceId.current());
    }

    @GetMapping("/exports/{exportId}/download")
    @Operation(summary = "下载本人未过期的成绩导出文件")
    public ResponseEntity<Resource> download(@PathVariable String exportId) {
        Long teacherId = AuthPrincipal.required().userId();
        String filename = service.filename(teacherId, exportId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8))
                .body(service.download(teacherId, exportId));
    }

    @GetMapping("/batches/{batchId}/insights")
    @Operation(operationId = "C06-1-01", summary = "读取平均分和要点漏点统计")
    public ApiResponse<InsightsView> insights(@PathVariable String batchId,
                                              @RequestParam(required = false) Integer rubricVersion) {
        return ApiResponse.ok(service.insights(batchId), TraceId.current());
    }
}
