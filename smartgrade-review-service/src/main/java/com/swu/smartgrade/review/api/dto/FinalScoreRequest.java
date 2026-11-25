package com.swu.smartgrade.review.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record FinalScoreRequest(
        @NotNull(message = "最终分不能为空") @DecimalMin(value = "0") BigDecimal finalScore,
        String reviewNote,
        @NotNull(message = "最终评语不能为空") String finalComment,
        Integer commentProfileVersion
) {
}
