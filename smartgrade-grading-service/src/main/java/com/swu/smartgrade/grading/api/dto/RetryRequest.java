package com.swu.smartgrade.grading.api.dto;

public record RetryRequest(
        // 需要重试的初筛任务标识。
        String taskId,
        // 教师填写的重试原因。
        String retryReason,
        // 幂等键，防止重复重试。
        String idempotencyKey
) {
    public RetryRequest(String taskId, String retryReason) {
        this(taskId, retryReason, null);
    }
}
