package com.swu.smartgrade.notification.api.dto;

import java.time.OffsetDateTime;

public record NotificationView(
        String notificationId,
        String type,
        String title,
        String content,
        String batchId,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
}
