package com.swu.smartgrade.review.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResultSummary(
        List<FinalizedScore> finalScores,
        List<PointMissCount> pointMissCounts,
        List<PointMissRate> pointMissRates
) {
    public record FinalizedScore(String answerId, String studentNo, BigDecimal finalScore, BigDecimal maxScore) {
    }

    public record PointMissCount(String pointId, String pointName, long missCount) {
    }

    public record PointMissRate(String pointId, String pointName, double missRate) {
    }
}
