package com.swu.smartgrade.review.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewStatusRequest(
        // 输入限制：复核状态不能为空。
        @NotBlank(message = "复核状态不能为空") String status,
        String reason
) {
}
