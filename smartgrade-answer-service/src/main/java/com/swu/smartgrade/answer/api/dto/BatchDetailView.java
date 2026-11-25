package com.swu.smartgrade.answer.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BatchDetailView(
        String batchId,
        String examName,
        String questionNo,
        String questionTitle,
        String questionText,
        BigDecimal maxScore,
        String status,
        OffsetDateTime createdAt,
        ProgressView progress,
        BatchView batch,
        Integer rubricVersion,
        BigDecimal baseScore
) {
    public BatchDetailView(String batchId, String examName, String questionNo, String questionTitle,
                           String questionText, BigDecimal maxScore, String status, OffsetDateTime createdAt,
                           ProgressView progress, BatchView batch, Integer rubricVersion) {
        this(batchId, examName, questionNo, questionTitle, questionText, maxScore, status, createdAt,
                progress, batch, rubricVersion, com.swu.smartgrade.common.util.ScorePolicy.baseScore(maxScore, null));
    }
}
