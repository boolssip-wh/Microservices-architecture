package com.swu.smartgrade.answer.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BatchView(
        String batchId,
        String examName,
        String questionNo,
        String questionTitle,
        BigDecimal maxScore,
        String status,
        OffsetDateTime createdAt,
        long answerCount,
        BigDecimal baseScore
) {
    public BatchView(String batchId, String examName, String questionNo, String questionTitle,
                     BigDecimal maxScore, String status, OffsetDateTime createdAt, long answerCount) {
        this(batchId, examName, questionNo, questionTitle, maxScore, status, createdAt, answerCount,
                com.swu.smartgrade.common.util.ScorePolicy.baseScore(maxScore, null));
    }
}
