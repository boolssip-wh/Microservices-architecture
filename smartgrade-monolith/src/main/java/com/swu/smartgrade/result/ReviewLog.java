package com.swu.smartgrade.result;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 教师复核/改分审计日志：原始分、最终分、原因、操作人，保证评分可追溯。
 */
@Entity
@Table(name = "review_log")
@Getter
@Setter
public class ReviewLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "old_score", nullable = false)
    private int oldScore;

    @Column(name = "new_score", nullable = false)
    private int newScore;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(length = 512)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
