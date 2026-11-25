package com.swu.smartgrade.assignment;

import com.swu.smartgrade.common.util.AnswerNormalizer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 单题作答与得分明细（评分结果服务的数据基础）。
 * 保留机器分、教师改分与评分依据，保证「可解释、可追溯」。
 */
@Entity
@Table(name = "answer_item")
@Getter
@Setter
public class AnswerItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "order_no", nullable = false)
    private int orderNo;

    @Column(name = "student_answer", length = 2000)
    private String studentAnswer;

    @Column(nullable = false)
    private int score;

    @Column(name = "full_score", nullable = false)
    private int fullScore;

    @Column(nullable = false)
    private boolean correct;

    @Column(length = 512)
    private String comment;

    /** 机器评分的置信度（主观题按关键词命中率计算） */
    private double confidence;

    /** MACHINE：自动判分；TEACHER：教师复核后改分 */
    @Column(name = "score_source", nullable = false, length = 16)
    private String scoreSource = "MACHINE";

    @Column(name = "matched_keywords", length = 512)
    private String matchedKeywords;

    public String normalizedAnswer() {
        return AnswerNormalizer.normalize(studentAnswer);
    }
}
