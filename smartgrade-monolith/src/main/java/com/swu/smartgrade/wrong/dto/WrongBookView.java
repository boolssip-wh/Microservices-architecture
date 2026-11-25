package com.swu.smartgrade.wrong.dto;

public record WrongBookView(
        Long questionId,
        String stem,
        String type,
        String knowledgeTag,
        int wrongTimes,
        Long lastSubmissionId,
        String lastAnswer,
        String standardAnswer,
        String analysis) {
}
