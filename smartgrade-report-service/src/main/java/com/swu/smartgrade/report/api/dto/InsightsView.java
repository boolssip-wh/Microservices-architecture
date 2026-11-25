package com.swu.smartgrade.report.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record InsightsView(
        BigDecimal averageScore,
        BigDecimal highestScore,
        BigDecimal lowestScore,
        BigDecimal maxScore,
        long reviewedCount,
        List<ScoreBand> scoreBands,
        List<PointInsight> pointInsights
) {
}
