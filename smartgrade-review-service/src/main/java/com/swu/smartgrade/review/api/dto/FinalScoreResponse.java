package com.swu.smartgrade.review.api.dto;

import java.math.BigDecimal;

public record FinalScoreResponse(
        String reviewId,
        BigDecimal finalScore,
        String snapshotId,
        String auditId
) {
}
