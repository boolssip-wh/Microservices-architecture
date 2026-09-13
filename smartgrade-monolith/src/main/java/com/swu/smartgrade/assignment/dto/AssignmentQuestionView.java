package com.swu.smartgrade.assignment.dto;

import java.util.List;

public record AssignmentQuestionView(
        Long questionId,
        int orderNo,
        int score,
        String type,
        String stem,
        List<String> options,
        String knowledgeTag) {
}
