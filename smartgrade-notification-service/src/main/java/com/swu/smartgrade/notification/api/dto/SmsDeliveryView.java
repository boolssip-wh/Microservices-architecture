package com.swu.smartgrade.notification.api.dto;

import java.time.OffsetDateTime;

public record SmsDeliveryView(
        String deliveryId,
        String eventType,
        String targetMasked,
        String message,
        String provider,
        String status,
        String errorMessage,
        OffsetDateTime createdAt) {
}
