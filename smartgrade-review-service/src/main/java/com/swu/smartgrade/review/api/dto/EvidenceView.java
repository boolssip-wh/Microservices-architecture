package com.swu.smartgrade.review.api.dto;

import java.math.BigDecimal;

public record EvidenceView(
        String evidenceId,
        String pointId,
        String pointName,
        String matchType,
        String sentence,
        int start,
        int end,
        BigDecimal confidence
) {
}
