package com.swu.smartgrade.grading.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record MatchRequest(
        String text,
        int rubricVersion,
        List<RubricPoint> points,
        BigDecimal answerCoverageRatio,
        BigDecimal maxScore,
        BigDecimal baseScore
) {
    public MatchRequest(String text, int rubricVersion, List<RubricPoint> points,
                        BigDecimal answerCoverageRatio, BigDecimal maxScore) {
        this(text, rubricVersion, points, answerCoverageRatio, maxScore, null);
    }
}
