package com.swu.smartgrade.assignment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AssignmentView(
        Long id,
        String title,
        String subject,
        Long classId,
        String className,
        Long teacherId,
        String teacherName,
        LocalDateTime deadline,
        String status,
        int fullScore,
        int questionCount,
        LocalDateTime createdAt,
        String mySubmissionStatus,
        Integer myScore,
        List<AssignmentQuestionView> questions) {
}
