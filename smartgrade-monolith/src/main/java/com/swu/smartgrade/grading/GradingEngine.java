package com.swu.smartgrade.grading;

import com.swu.smartgrade.bank.Question;
import com.swu.smartgrade.bank.QuestionType;
import com.swu.smartgrade.common.util.AnswerNormalizer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 判分规则引擎（纯函数，无数据库依赖，便于单元测试与后续替换成 AI 评分）。
 *
 * <ul>
 *   <li>单选 / 判断：归一化后精确匹配</li>
 *   <li>多选：选项无序集合比对，漏选给半分，错选不得分</li>
 *   <li>填空：支持「|」分隔的多种可接受写法，去空格与全角差异</li>
 *   <li>主观题：关键词命中率给分，并返回命中/未命中关键词，供教师复核</li>
 * </ul>
 */
@Component
public class GradingEngine {

    public GradeOutcome grade(Question question, String studentAnswer, int fullScore) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return new GradeOutcome(0, fullScore, false, "未作答", 0.0, "");
        }
        return switch (question.getType()) {
            case SINGLE_CHOICE -> gradeExact(studentAnswer, question.getAnswerKey(), fullScore, "单选题归一化后精确匹配");
            case JUDGE -> gradeJudge(studentAnswer, question.getAnswerKey(), fullScore);
            case MULTIPLE_CHOICE -> gradeMultiple(studentAnswer, question.getAnswerKey(), fullScore);
            case FILL_BLANK -> gradeFillBlank(studentAnswer, question.getAnswerKey(), fullScore);
            case SUBJECTIVE -> gradeSubjective(studentAnswer, question.getKeywords(), question.getAnswerKey(), fullScore);
        };
    }

    private GradeOutcome gradeExact(String studentAnswer, String answerKey, int fullScore, String ruleNote) {
        boolean correct = AnswerNormalizer.normalize(studentAnswer).equals(AnswerNormalizer.normalize(answerKey));
        return new GradeOutcome(correct ? fullScore : 0, fullScore, correct,
                correct ? "正确（" + ruleNote + "）" : "与标准答案不一致（" + ruleNote + "）",
                correct ? 1.0 : 0.0, "");
    }

    private GradeOutcome gradeJudge(String studentAnswer, String answerKey, int fullScore) {
        boolean correct = AnswerNormalizer.normalizeJudge(studentAnswer)
                .equals(AnswerNormalizer.normalizeJudge(answerKey));
        return new GradeOutcome(correct ? fullScore : 0, fullScore, correct,
                correct ? "判断题正确（正确/错误、对/错、√/× 已归一化）" : "判断题与标准答案不一致",
                correct ? 1.0 : 0.0, "");
    }

    private GradeOutcome gradeMultiple(String studentAnswer, String answerKey, int fullScore) {
        Set<String> student = AnswerNormalizer.normalizeOptionSet(studentAnswer);
        Set<String> standard = AnswerNormalizer.normalizeOptionSet(answerKey);
        if (student.equals(standard)) {
            return new GradeOutcome(fullScore, fullScore, true, "多选题选项完全一致（顺序不影响判分）", 1.0, "");
        }
        boolean hasWrongOption = student.stream().anyMatch(option -> !standard.contains(option));
        if (!student.isEmpty() && !hasWrongOption) {
            int partial = Math.max(1, fullScore / 2);
            return new GradeOutcome(partial, fullScore, false,
                    "漏选（未选中 " + diff(standard, student) + "），按半分处理", 0.5, "");
        }
        return new GradeOutcome(0, fullScore, false,
                hasWrongOption ? "存在错误选项（" + diff(student, standard) + "）" : "未作答", 0.0, "");
    }

    private GradeOutcome gradeFillBlank(String studentAnswer, String answerKey, int fullScore) {
        String normalizedStudent = AnswerNormalizer.normalize(studentAnswer);
        List<String> accepted = Arrays.stream(answerKey.split("\\|"))
                .map(AnswerNormalizer::normalize)
                .filter(key -> !key.isBlank())
                .toList();
        boolean correct = accepted.contains(normalizedStudent);
        return new GradeOutcome(correct ? fullScore : 0, fullScore, correct,
                correct ? "填空题正确（已忽略空格、大小写与全角差异）"
                        : "与可接受答案不一致，标准答案：" + answerKey,
                correct ? 1.0 : 0.0, "");
    }

    private GradeOutcome gradeSubjective(String studentAnswer, String keywords, String answerKey, int fullScore) {
        if (keywords == null || keywords.isBlank()) {
            return new GradeOutcome(0, fullScore, false,
                    "该题未配置关键词评分规则，需教师人工评分", 0.0, "");
        }
        String normalizedStudent = AnswerNormalizer.normalize(studentAnswer);
        List<String> allKeywords = Arrays.stream(keywords.split("\\|"))
                .map(String::trim)
                .filter(keyword -> !keyword.isBlank())
                .toList();
        List<String> matched = new ArrayList<>();
        List<String> missed = new ArrayList<>();
        for (String keyword : allKeywords) {
            if (normalizedStudent.contains(AnswerNormalizer.normalize(keyword))) {
                matched.add(keyword);
            } else {
                missed.add(keyword);
            }
        }
        double ratio = allKeywords.isEmpty() ? 0.0 : (double) matched.size() / allKeywords.size();
        int score = (int) Math.round(fullScore * ratio);
        boolean correct = ratio >= 0.6;
        String comment = "关键词命中 " + matched.size() + "/" + allKeywords.size()
                + "；命中：" + (matched.isEmpty() ? "无" : String.join("、", matched))
                + "；未命中：" + (missed.isEmpty() ? "无" : String.join("、", missed))
                + "；参考要点：" + answerKey;
        return new GradeOutcome(score, fullScore, correct, comment, round(ratio), String.join("|", matched));
    }

    private String diff(Set<String> left, Set<String> right) {
        return left.stream().filter(item -> !right.contains(item)).sorted().reduce((a, b) -> a + "," + b).orElse("");
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
