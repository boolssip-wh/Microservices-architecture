package com.swu.smartgrade.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NotificationChannelRequest(
        @NotBlank(message = "接收方式不能为空")
        String contact,
        @NotNull(message = "通知开关不能为空")
        Boolean enabled) {
}
