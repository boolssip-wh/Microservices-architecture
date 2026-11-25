package com.swu.smartgrade.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NotificationChannelRequest(
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^(?:\\+?86)?1[3-9]\\d{9}$", message = "请输入有效的中国大陆手机号")
        String phone,
        @NotNull(message = "通知开关不能为空")
        Boolean enabled) {
}
