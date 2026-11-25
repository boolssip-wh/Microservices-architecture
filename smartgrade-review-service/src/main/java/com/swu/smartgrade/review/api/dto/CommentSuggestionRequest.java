package com.swu.smartgrade.review.api.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CommentSuggestionRequest(
        @NotNull(message = "最终分不能为空") BigDecimal finalScore,
        Integer profileVersion
) {
}
