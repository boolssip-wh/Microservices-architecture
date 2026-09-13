package com.swu.smartgrade.report.dto;

import java.util.List;

/**
 * 班级看板（报表服务）：提交率、平均分、分数分布、错题排行与知识点薄弱分析。
 */
public record AssignmentReport(
        Long assignmentId,
        String title,
        Long classId,
        String className,
        int fullScore,
        int studentCount,
        int submittedCount,
        double submitRate,
        double averageScore,
        int highestScore,
        int lowestScore,
        List<ScoreBucket> distribution,
        List<QuestionWrongRank> wrongRank,
        List<KnowledgeWeakness> weakness) {

    public record ScoreBucket(String label, int count) {
    }

    public record QuestionWrongRank(Long questionId, String stem, String knowledgeTag, long wrongCount, double wrongRate) {
    }

    public record KnowledgeWeakness(String knowledgeTag, long wrongCount, long answerCount, double wrongRate) {
    }
}
