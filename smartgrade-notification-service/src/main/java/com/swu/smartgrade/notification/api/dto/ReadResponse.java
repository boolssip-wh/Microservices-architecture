package com.swu.smartgrade.notification.api.dto;

import java.time.OffsetDateTime;

public record ReadResponse(String notificationId, OffsetDateTime readAt) {
}
