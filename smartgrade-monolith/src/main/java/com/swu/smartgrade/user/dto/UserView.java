package com.swu.smartgrade.user.dto;

public record UserView(Long id, String username, String realName, String role, Long classId, String className) {
}
