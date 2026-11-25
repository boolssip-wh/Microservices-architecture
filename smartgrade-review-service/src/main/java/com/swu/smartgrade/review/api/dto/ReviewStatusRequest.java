package com.swu.smartgrade.review.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewStatusRequest(
        @NotBlank(message = "复核状态不能为空") String status,
        String reason
) {
}
