package com.swu.smartgrade.notification.api.dto;

/**
 * A4/C07 内部事件契约：业务服务提交教师、事件类型、标题、正文和批次 ID，通知服务据此落库并推送。
 */
public record CreateNotificationRequest(
        Long teacherId,
        String type,
        String title,
        String content,
        String batchId
) {
}
