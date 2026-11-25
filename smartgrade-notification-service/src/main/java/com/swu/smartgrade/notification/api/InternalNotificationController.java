package com.swu.smartgrade.notification.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.notification.api.dto.CreateNotificationRequest;
import com.swu.smartgrade.notification.api.dto.NotificationView;
import com.swu.smartgrade.notification.application.NotificationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "C07 任务通知服务")
@RequestMapping("/internal/notifications")
public class InternalNotificationController {
    private final NotificationApplicationService service;

    public InternalNotificationController(NotificationApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(operationId = "C07-1-01", summary = "供业务服务写入站内通知")
    public ApiResponse<NotificationView> create(@RequestBody CreateNotificationRequest request) {
        return ApiResponse.ok(service.create(request), TraceId.current());
    }

    @PostMapping("/events")
    @Operation(summary = "按事件编号幂等接收本地模式的任务事件")
    public ApiResponse<NotificationView> consume(@RequestBody com.swu.smartgrade.common.event.DomainEvent event) {
        return ApiResponse.ok(service.consume(event), TraceId.current());
    }
}
