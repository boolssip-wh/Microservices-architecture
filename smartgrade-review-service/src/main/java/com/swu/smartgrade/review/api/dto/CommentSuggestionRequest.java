package com.swu.smartgrade.review.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CommentSuggestionRequest(
        @NotNull(message = "最终分不能为空") BigDecimal finalScore,
        Integer profileVersion,
        @Size(max = 2000, message = "教师口述要求不能超过2000字")
        String teacherInstruction
) {
}
