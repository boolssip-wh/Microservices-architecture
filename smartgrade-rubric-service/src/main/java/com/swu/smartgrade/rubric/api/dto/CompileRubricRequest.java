package com.swu.smartgrade.rubric.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;

public record CompileRubricRequest(@NotNull @DecimalMin("100") @DecimalMax("100") BigDecimal expectedWeightTotal) {
}
