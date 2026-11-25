package com.swu.smartgrade.review.api.dto;

import java.math.BigDecimal;

public record ResultListItem(
        String answerId,
        String studentNo,
        BigDecimal suggestedScore,
        BigDecimal finalScore,
        BigDecimal maxScore,
        BigDecimal confidence,
        String status
) {
}
