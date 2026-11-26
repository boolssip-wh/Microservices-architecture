package com.swu.smartgrade.grading.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record MatchRequest(
        // 识别后的作答文本。
        String text,
        // 本次匹配使用的冻结规则版本。
        int rubricVersion,
        // 规则要点及其关键词、同义表达。
        List<RubricPoint> points,
        // 视觉模型估算的作答区覆盖率。
        BigDecimal answerCoverageRatio,
        // 批次满分。
        BigDecimal maxScore,
        // 批次基础分。
        BigDecimal baseScore
) {
    public MatchRequest(String text, int rubricVersion, List<RubricPoint> points,
                        BigDecimal answerCoverageRatio, BigDecimal maxScore) {
        this(text, rubricVersion, points, answerCoverageRatio, maxScore, null);
    }
}
