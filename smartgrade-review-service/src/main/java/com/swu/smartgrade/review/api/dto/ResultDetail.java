package com.swu.smartgrade.review.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResultDetail(
        String resultId,
        String answerId,
        String studentNo,
        String answerText,
        int rubricVersion,
        BigDecimal ocrConfidence,
        BigDecimal suggestedScore,
        BigDecimal finalScore,
        BigDecimal maxScore,
        BigDecimal answerCoverageRatio,
        String status,
        List<EvidenceView> evidence,
        CommentView comment,
        BigDecimal baseScore
) {
    public ResultDetail(String resultId, String answerId, String studentNo, String answerText,
                        int rubricVersion, BigDecimal ocrConfidence, BigDecimal suggestedScore,
                        BigDecimal finalScore, BigDecimal maxScore, BigDecimal answerCoverageRatio,
                        String status, List<EvidenceView> evidence, CommentView comment) {
        this(resultId, answerId, studentNo, answerText, rubricVersion, ocrConfidence, suggestedScore,
                finalScore, maxScore, answerCoverageRatio, status, evidence, comment,
                com.swu.smartgrade.common.util.ScorePolicy.baseScore(maxScore, null));
    }
}
