package com.swu.smartgrade.assignment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 一次作业提交（作业服务）：保存任务状态与总分，是幂等键的载体。
 */
@Entity
@Table(name = "submission")
@Getter
@Setter
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SubmissionStatus status = SubmissionStatus.GRADING;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "full_score", nullable = false)
    private int fullScore;

    /** 幂等键：同一学生重复请求（网络重试）只会产生一条提交记录 */
    @Column(name = "submit_key", length = 64, unique = true)
    private String submitKey;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "reviewed_by")
    private Long reviewedBy;
}
