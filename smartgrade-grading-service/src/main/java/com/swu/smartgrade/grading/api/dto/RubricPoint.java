package com.swu.smartgrade.grading.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record RubricPoint(
        String pointId,
        String pointName,
        String pointType,
        List<String> keywords,
        List<String> variants,
        BigDecimal weight,
        BigDecimal maxScore
) {
}
