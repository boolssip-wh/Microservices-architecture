package com.swu.smartgrade.answer.api;

import com.swu.smartgrade.answer.application.BatchApplicationService;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A4/B2 内部接口：供其他服务按 batchId 读取批次满分和基础分。
 * /internal/** 必须携带服务令牌，不直接暴露给浏览器。
 */
@RestController
@RequestMapping("/internal/batches")
public class InternalBatchController {
    private final BatchApplicationService service;

    public InternalBatchController(BatchApplicationService service) {
        this.service = service;
    }

    @GetMapping("/{batchId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String batchId) {
        Long teacherId = AuthPrincipal.required().userId();
        var batch = service.requireOwned(teacherId, batchId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchId", batch.getBatchId());
        result.put("teacherId", batch.getTeacherId());
        result.put("maxScore", batch.getMaxScore());
        result.put("baseScore", com.swu.smartgrade.common.util.ScorePolicy.baseScore(
                batch.getMaxScore(), batch.getBaseScore()));
        return ApiResponse.ok(result, TraceId.current());
    }
}
