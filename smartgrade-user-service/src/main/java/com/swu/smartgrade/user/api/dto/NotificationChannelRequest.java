package com.swu.smartgrade.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record NotificationChannelRequest(
        // 输入限制：接收方式必须为手机号或邮箱，不能为空。
        @NotBlank(message = "接收方式不能为空")
        String contact,
        // 输入限制：是否开启通知必须明确给出 true/false。
        @NotNull(message = "通知开关不能为空")
        Boolean enabled) {
}
