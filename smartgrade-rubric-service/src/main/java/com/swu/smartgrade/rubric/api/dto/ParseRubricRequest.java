package com.swu.smartgrade.rubric.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParseRubricRequest(
        // 输入限制：待解析的评分细则 20-10000 字。
        @NotBlank @Size(min = 20, max = 10000) String sourceText
) {
}
