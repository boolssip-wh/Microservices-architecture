package com.swu.smartgrade.user.api.dto;

public record CurrentUserResponse(Long teacherId, String name, String username, String role,
                                  String displayName, java.time.OffsetDateTime expiresAt) {
}
