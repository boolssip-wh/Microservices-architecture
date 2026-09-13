package com.swu.smartgrade.bank;

/**
 * 题型：客观题（可全自动判分）+ 填空题（归一化比对）+ 主观题（关键词规则辅助评分）。
 */
public enum QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    JUDGE,
    FILL_BLANK,
    SUBJECTIVE
}
