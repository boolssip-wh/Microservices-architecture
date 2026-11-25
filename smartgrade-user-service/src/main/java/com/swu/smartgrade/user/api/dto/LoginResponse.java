package com.swu.smartgrade.user.api.dto;

import java.time.OffsetDateTime;

public record LoginResponse(
        String accessToken,
        String role,
        OffsetDateTime expiresAt,
        Long teacherId,
        String name
) {
}
