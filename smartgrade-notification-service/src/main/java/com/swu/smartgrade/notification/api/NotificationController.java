package com.swu.smartgrade.notification.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.notification.api.dto.NotificationPage;
import com.swu.smartgrade.notification.api.dto.ReadResponse;
import com.swu.smartgrade.notification.application.NotificationApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@Tag(name = "C07 任务通知服务")
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationApplicationService service;

    public NotificationController(NotificationApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "C07-3-01", summary = "分页读取本人站内通知")
    public ApiResponse<NotificationPage> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(AuthPrincipal.required().userId(), unreadOnly, page, size),
                TraceId.current());
    }

    @PutMapping("/{notificationId}/read")
    @Operation(operationId = "C07-4-01", summary = "将本人通知标记为已读")
    public ApiResponse<ReadResponse> markRead(@PathVariable String notificationId) {
        return ApiResponse.ok(service.markRead(AuthPrincipal.required().userId(), notificationId),
                TraceId.current());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(operationId = "C07-2-01", summary = "建立 SSE 实时通知流")
    public SseEmitter stream() {
        return service.stream(AuthPrincipal.required().userId());
    }
}
