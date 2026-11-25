package com.swu.smartgrade.grading.api.dto;

public record TaskView(
        String taskId,
        String answerId,
        String status,
        int retryCount,
        String errorCode
) {
}
