package com.swu.smartgrade.rubric.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;
import java.util.List;

public record RubricPointRequest(
        @Size(max = 64) String pointId,
        @NotBlank(message = "要点名称不能为空") @Size(min = 2, max = 50) String pointName,
        @NotBlank @Pattern(regexp = "CONCEPT|PRINCIPLE|SIGNIFICANCE|APPLICATION|COUNTERMEASURE|EXTENSION|KEYWORD|LENGTH_COVERAGE") String pointType,
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 30) String> keywords,
        @Size(max = 30) List<@NotBlank @Size(max = 30) String> variants,
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal weight,
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal maxScore
) {
    public RubricPointRequest {
        keywords = keywords == null ? null : keywords.stream().distinct().toList();
        variants = variants == null ? List.of() : variants.stream().distinct().toList();
    }
}
