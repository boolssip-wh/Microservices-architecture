package com.swu.smartgrade.grading.api.dto;

import java.math.BigDecimal;

public record HitView(
        String pointId,
        String pointName,
        String matchType,
        String evidence,
        int start,
        int end,
        BigDecimal confidence
) {
}
