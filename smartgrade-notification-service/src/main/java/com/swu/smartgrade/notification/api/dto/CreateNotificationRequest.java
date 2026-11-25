package com.swu.smartgrade.notification.api.dto;

public record CreateNotificationRequest(
        Long teacherId,
        String type,
        String title,
        String content,
        String batchId
) {
}
