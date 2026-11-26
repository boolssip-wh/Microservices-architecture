package com.swu.smartgrade.grading.api.dto;

public record OcrRequest(
        // 答卷唯一标识，用于关联任务和回写结果。
        String answerId,
        // 图片或 PDF 的对象存储键；文本直读时可为空。
        String objectKey,
        // 文本直读时的原文；图片识别时可为空。
        String text
) {
}
