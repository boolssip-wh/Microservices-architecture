package com.swu.smartgrade.rubric.infrastructure;

import com.swu.smartgrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "smartgrade-answer-service")
public interface BatchAccessClient {
    @GetMapping("/api/batches/{batchId}")
    ApiResponse<java.util.Map<String, Object>> detail(@PathVariable String batchId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestHeader("X-Role") String role,
            @RequestHeader("X-Trace-Id") String traceId,
            @RequestHeader("X-Service-Token") String serviceToken);
}
