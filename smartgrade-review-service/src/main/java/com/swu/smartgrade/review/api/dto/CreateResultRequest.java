package com.swu.smartgrade.review.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateResultRequest(
        String answerId,
        String batchId,
        String studentNo,
        String answerText,
        Long teacherId,
        int rubricVersion,
        BigDecimal suggestedScore,
        BigDecimal maxScore,
        BigDecimal answerCoverageRatio,
        BigDecimal confidence,
        List<HitInput> hits,
        String status,
        List<ScoringPoint> scoringPoints,
        BigDecimal baseScore
) {
    public CreateResultRequest(String answerId, String batchId, String studentNo, String answerText,
                               Long teacherId, int rubricVersion, BigDecimal suggestedScore,
                               BigDecimal maxScore, BigDecimal answerCoverageRatio,
                               BigDecimal confidence, List<HitInput> hits) {
        this(answerId, batchId, studentNo, answerText, teacherId, rubricVersion, suggestedScore,
                maxScore, answerCoverageRatio, confidence, hits, "WAITING_REVIEW", List.of(), null);
    }
    public CreateResultRequest(String answerId, String batchId, String studentNo, String answerText,
                               Long teacherId, int rubricVersion, BigDecimal suggestedScore,
                               BigDecimal maxScore, BigDecimal answerCoverageRatio, BigDecimal confidence,
                               List<HitInput> hits, String status, List<ScoringPoint> scoringPoints) {
        this(answerId, batchId, studentNo, answerText, teacherId, rubricVersion, suggestedScore,
                maxScore, answerCoverageRatio, confidence, hits, status, scoringPoints, null);
    }
    public record ScoringPoint(String pointId, String pointName) { }
}
