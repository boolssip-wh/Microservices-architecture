package com.swu.smartgrade.user.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.user.api.dto.NotificationChannelRequest;
import com.swu.smartgrade.user.api.dto.NotificationChannelView;
import com.swu.smartgrade.user.application.NotificationChannelApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers/me/notification-channel")
@Tag(name = "C01 身份与教师偏好服务")
public class TeacherNotificationChannelController {
    private final NotificationChannelApplicationService service;

    public TeacherNotificationChannelController(NotificationChannelApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "C01-5-01", summary = "读取教师短信通知绑定状态")
    public ApiResponse<NotificationChannelView> get() {
        return ApiResponse.ok(service.getFor(AuthPrincipal.required()), TraceId.current());
    }

    @PutMapping
    @Operation(operationId = "C01-5-02", summary = "绑定手机并设置短信通知开关")
    public ApiResponse<NotificationChannelView> save(@Valid @RequestBody NotificationChannelRequest request) {
        return ApiResponse.ok(service.saveFor(AuthPrincipal.required(), request), TraceId.current());
    }
}
