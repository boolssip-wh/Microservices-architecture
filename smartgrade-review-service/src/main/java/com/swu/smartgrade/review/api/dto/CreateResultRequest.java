package com.swu.smartgrade.review.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * A4/D1 跨服务写入契约：answer-service 将初筛结果、证据、建议分和规则要点目录投递给 review-service。
 * 该字段集合对应 review_result_projection 与 review_evidence_projection 两张表。
 */
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
    /**
     * 评分要点目录：只保留 pointId 和名称，用于报告统计，不复制 rubric-service 的关键词等完整规则。
     */
    public record ScoringPoint(String pointId, String pointName) { }
}
