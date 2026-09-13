package com.swu.smartgrade.bank.dto;

/**
 * 题目视图。学生视角不下发 answerKey/analysis，避免提前看到答案。
 */
public record QuestionView(
        Long id,
        String subject,
        String knowledgeTag,
        String type,
        String stem,
        java.util.List<String> options,
        String answerKey,
        java.util.List<String> keywords,
        int defaultScore,
        String analysis,
        int difficulty) {
}
