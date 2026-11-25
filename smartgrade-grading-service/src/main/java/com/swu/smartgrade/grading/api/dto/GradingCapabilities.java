package com.swu.smartgrade.grading.api.dto;

import java.math.BigDecimal;

public record GradingCapabilities(
        BigDecimal baseScore,
        BigDecimal minimumCoverage,
        int minimumTextCharacters,
        boolean roundToInteger,
        String ocrProvider,
        String ocrModel,
        boolean ocrConfigured
) {
}
