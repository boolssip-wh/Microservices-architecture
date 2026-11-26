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
        // 已有要点可传 pointId；新建时为空，最长 64 位。
        @Size(max = 64) String pointId,
        // 输入限制：要点名称 2-50 字。
        @NotBlank(message = "要点名称不能为空") @Size(min = 2, max = 50) String pointName,
        // 输入限制：要点类型只允许文档约定的八类。
        @NotBlank @Pattern(regexp = "CONCEPT|PRINCIPLE|SIGNIFICANCE|APPLICATION|COUNTERMEASURE|EXTENSION|KEYWORD|LENGTH_COVERAGE") String pointType,
        // 输入限制：关键词最多 20 个，每个最多 30 字。
        @NotNull @Size(max = 20) List<@NotBlank @Size(max = 30) String> keywords,
        // 输入限制：同义表达最多 30 个，每个最多 30 字。
        @Size(max = 30) List<@NotBlank @Size(max = 30) String> variants,
        // 输入限制：权重 0-100，最多 3 位整数、2 位小数。
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal weight,
        // 输入限制：分值 0-100，最多 3 位整数、2 位小数。
        @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal maxScore
) {
    public RubricPointRequest {
        keywords = keywords == null ? null : keywords.stream().distinct().toList();
        variants = variants == null ? List.of() : variants.stream().distinct().toList();
    }
}
