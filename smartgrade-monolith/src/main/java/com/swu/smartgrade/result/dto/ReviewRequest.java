package com.swu.smartgrade.result.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewRequest(
        @NotNull(message = "题目不能为空") Long questionId,
        @NotNull(message = "分数不能为空") Integer score,
        String reason) {
}
