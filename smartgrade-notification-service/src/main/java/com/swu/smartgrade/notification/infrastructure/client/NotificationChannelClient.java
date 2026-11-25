package com.swu.smartgrade.notification.infrastructure.client;

import com.swu.smartgrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "smartgrade-user-service")
public interface NotificationChannelClient {
    @GetMapping("/internal/teachers/{teacherId}/notification-channel")
    ApiResponse<Channel> get(
            @PathVariable Long teacherId,
            @RequestHeader("X-Service-Token") String serviceToken);

    record Channel(boolean bound, boolean enabled, String phone) {
    }
}
