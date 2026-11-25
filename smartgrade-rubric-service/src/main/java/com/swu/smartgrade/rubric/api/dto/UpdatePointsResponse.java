package com.swu.smartgrade.rubric.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record UpdatePointsResponse(String rubricId, List<RubricPointView> points, BigDecimal weightTotal) {
}
