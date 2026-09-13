package com.swu.smartgrade.grading;

/**
 * 单题判分结果：分数、对错、给分说明、置信度与命中的关键词。
 * comment/confidence 是「可解释评分」的落点，教师复核时能看到机器为什么这样给分。
 */
public record GradeOutcome(
        int score,
        int fullScore,
        boolean correct,
        String comment,
        double confidence,
        String matchedKeywords) {
}
