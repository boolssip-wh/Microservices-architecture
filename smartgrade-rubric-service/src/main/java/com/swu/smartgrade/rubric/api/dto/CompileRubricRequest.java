package com.swu.smartgrade.rubric.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

public record CompileRubricRequest(
        // 输入限制：冻结规则前，权重合计必须恰好为 100。
        @NotNull @DecimalMin("100") @DecimalMax("100") BigDecimal expectedWeightTotal
) {
}
