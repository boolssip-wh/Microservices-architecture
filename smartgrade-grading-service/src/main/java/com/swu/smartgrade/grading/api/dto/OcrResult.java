package com.swu.smartgrade.grading.api.dto;

import java.math.BigDecimal;

public record OcrResult(
        String text,
        BigDecimal confidence,
        BigDecimal answerCoverageRatio,
        int characterCount,
        String provider,
        String failureReason,
        long durationMs
) {
}
