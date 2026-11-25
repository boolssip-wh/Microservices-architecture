package com.swu.smartgrade.grading.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record MatchResult(
        List<HitView> hits,
        BigDecimal suggestedScore,
        BigDecimal confidence
) {
}
