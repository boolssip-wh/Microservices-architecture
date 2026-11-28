package com.swu.smartgrade.answer.api;

import com.swu.smartgrade.answer.api.dto.ValidationView;
import com.swu.smartgrade.answer.application.AnswerImportApplicationService;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A4/C03 接口入口：按 jobId 返回导入校验结果。
 */
@RestController
@io.swagger.v3.oas.annotations.tags.Tag(name="C03 答卷批次服务")
@RequestMapping("/api/import-jobs")
public class ImportJobController {
    private final AnswerImportApplicationService service;

    public ImportJobController(AnswerImportApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{jobId}/validation")
    @io.swagger.v3.oas.annotations.Operation(operationId="C03-3-01", summary="100% 标出重复空白损坏文件")
    public ApiResponse<ValidationView> validation(@PathVariable String jobId) {
        return ApiResponse.ok(service.validation(com.swu.smartgrade.common.security.AuthPrincipal.required().userId(), jobId), TraceId.current());
    }
}
