package com.swu.smartgrade.rubric.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public record CreateRubricRequest(
        // 输入限制：批次编号不能为空，最大 64 位。
        @NotBlank(message = "批次编号不能为空") @Size(max = 64) String batchId,
        // 输入限制：评分细则 20-10000 字。
        @NotBlank(message = "评分细则不能为空") @Size(min = 20, max = 10000) String sourceText,
        // 输入限制：满分 0.01-100，最多 3 位整数、2 位小数。
        @NotNull(message = "满分不能为空") @DecimalMin(value = "0.01") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal maxScore,
        // 输入限制：基础分不能为负数，最多 3 位整数、2 位小数。
        @DecimalMin("0") @Digits(integer = 3, fraction = 2) BigDecimal baseScore
) {
    public CreateRubricRequest(String batchId, String sourceText, BigDecimal maxScore) {
        this(batchId, sourceText, maxScore, null);
    }
}
