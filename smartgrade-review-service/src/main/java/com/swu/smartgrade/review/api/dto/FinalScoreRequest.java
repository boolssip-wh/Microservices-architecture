package com.swu.smartgrade.review.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinalScoreRequest(
        // 输入限制：最终分不能为空且不能为负数。
        @NotNull(message = "最终分不能为空") @DecimalMin(value = "0") BigDecimal finalScore,
        String reviewNote,
        // 输入限制：最终评语不能为空。
        @NotNull(message = "最终评语不能为空") String finalComment,
        Integer commentProfileVersion
) {
}
