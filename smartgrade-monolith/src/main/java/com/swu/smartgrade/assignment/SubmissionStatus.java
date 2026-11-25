package com.swu.smartgrade.assignment;

/**
 * 提交状态机：GRADING → GRADED →（教师改分后）REVIEWED。
 * 第 7 周用 Seata 保证「提交记录 + 任务状态 + 结果写入」一致时，状态流转保持不变。
 */
public enum SubmissionStatus {
    GRADING,
    GRADED,
    REVIEWED
}
