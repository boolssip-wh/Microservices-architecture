package com.swu.smartgrade.grading.api.dto;

public record RetryRequest(String taskId, String retryReason, String idempotencyKey) {
    public RetryRequest(String taskId, String retryReason) {
        this(taskId, retryReason, null);
    }
}
