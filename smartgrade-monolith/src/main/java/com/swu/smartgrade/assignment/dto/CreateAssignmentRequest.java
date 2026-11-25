package com.swu.smartgrade.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

public record CreateAssignmentRequest(
        @NotBlank(message = "作业标题不能为空") String title,
        String subject,
        Long classId,
        LocalDateTime deadline,
        @NotEmpty(message = "至少选择一道题目") List<AssignmentItem> questions) {

    public record AssignmentItem(Long questionId, Integer score) {
    }
}
