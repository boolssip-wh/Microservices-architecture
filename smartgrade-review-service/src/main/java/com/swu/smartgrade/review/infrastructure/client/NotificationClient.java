package com.swu.smartgrade.review.infrastructure.client;

import com.swu.smartgrade.common.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "smartgrade-notification-service")
public interface NotificationClient {
    @PostMapping("/internal/notifications")
    ApiResponse<CreatedNotification> create(@RequestBody CreateNotificationRequest request);

    record CreateNotificationRequest(Long teacherId, String type, String title, String content, String batchId) {
    }

    record CreatedNotification(String notificationId) {
    }
}
