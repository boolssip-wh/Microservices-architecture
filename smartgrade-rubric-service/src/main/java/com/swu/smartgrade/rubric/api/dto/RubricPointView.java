package com.swu.smartgrade.rubric.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record RubricPointView(
        String pointId,
        String pointCode,
        String pointName,
        String pointType,
        List<String> keywords,
        List<String> variants,
        BigDecimal weight,
        BigDecimal maxScore
) {
}
