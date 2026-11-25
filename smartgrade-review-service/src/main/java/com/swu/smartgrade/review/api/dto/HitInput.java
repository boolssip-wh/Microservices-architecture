package com.swu.smartgrade.review.api.dto;

import java.math.BigDecimal;

public record HitInput(
        String pointId,
        String pointName,
        String matchType,
        String evidence,
        int start,
        int end,
        BigDecimal confidence
) {
}
