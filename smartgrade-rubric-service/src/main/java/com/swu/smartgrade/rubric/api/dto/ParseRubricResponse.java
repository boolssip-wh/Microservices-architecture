package com.swu.smartgrade.rubric.api.dto;

import java.util.List;

public record ParseRubricResponse(
        List<RubricPointView> points,
        List<java.math.BigDecimal> suggestedWeights,
        List<String> unconfirmed
) {
}
