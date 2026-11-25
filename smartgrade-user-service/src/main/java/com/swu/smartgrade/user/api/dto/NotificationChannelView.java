package com.swu.smartgrade.user.api.dto;

import java.time.OffsetDateTime;

public record NotificationChannelView(
        Long teacherId,
        boolean bound,
        String maskedPhone,
        boolean enabled,
        OffsetDateTime updatedAt) {
}
