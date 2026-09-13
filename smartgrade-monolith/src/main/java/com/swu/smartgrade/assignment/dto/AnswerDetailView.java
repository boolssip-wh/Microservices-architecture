package com.swu.smartgrade.assignment.dto;

public record AnswerDetailView(
        Long questionId,
        int orderNo,
        String type,
        String stem,
        String knowledgeTag,
        String studentAnswer,
        String standardAnswer,
        String analysis,
        int score,
        int fullScore,
        boolean correct,
        String comment,
        double confidence,
        String scoreSource,
        String matchedKeywords) {
}
