package com.swu.smartgrade.answer.api.dto;

public record ImportResult(
        String jobId,
        int acceptedCount,
        int rejectedCount,
        String status
) {
}
