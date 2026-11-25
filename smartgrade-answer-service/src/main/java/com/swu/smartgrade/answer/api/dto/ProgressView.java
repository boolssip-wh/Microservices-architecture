package com.swu.smartgrade.answer.api.dto;

public record ProgressView(
        long total,
        long pending,
        long running,
        long waitingReview,
        long manualReview,
        long failed
) {
}
