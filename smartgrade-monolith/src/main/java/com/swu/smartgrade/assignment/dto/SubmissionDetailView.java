package com.swu.smartgrade.assignment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SubmissionDetailView(
        Long submissionId,
        Long assignmentId,
        String assignmentTitle,
        Long studentId,
        String studentName,
        String status,
        int totalScore,
        int fullScore,
        LocalDateTime submittedAt,
        LocalDateTime gradedAt,
        List<AnswerDetailView> answers) {
}
