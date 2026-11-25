package com.swu.smartgrade.answer.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AnswerView(
        String answerId,
        String studentNo,
        String sourceType,
        String processStatus,
        BigDecimal ocrConfidence,
        BigDecimal ocrCoverageRatio,
        Integer ocrCharacterCount,
        OffsetDateTime createdAt,
        String taskId
) {
}
