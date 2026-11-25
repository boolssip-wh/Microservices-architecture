package com.swu.smartgrade.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @Pattern(regexp = "[A-Za-z0-9_]{4,32}", message = "用户名须为4至32位字母、数字或下划线") @NotBlank String username,
        @Pattern(regexp = "(?=.{8,20}$)(?=.*[A-Za-z])(?=.*[0-9])\\S+", message = "密码须为8至20位，包含字母和数字且不得含空格") @NotBlank String password
) {
    public LoginRequest {
        username = username == null ? null : username.trim();
    }
}
