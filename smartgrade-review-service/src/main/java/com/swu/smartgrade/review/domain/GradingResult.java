package com.swu.smartgrade.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A5 复核结果投影：answer-service 通过 D1 跨服务事务写入。
 * answerId 唯一，保证同一份答卷不会被重复创建复核结果。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "review_result_projection")
public class GradingResult {
    @Id
    private String resultId;
    @Column(unique = true) // 一份答卷对应零或一条复核结果
    private String answerId;
    private String batchId; // 逻辑外键 -> answer-service.batch
    private String studentNo;
    @Column(columnDefinition = "TEXT")
    private String answerText;
    private Long teacherId;
    private Integer rubricVersion;
    @Column(columnDefinition = "TEXT")
    private String scoringPointsJson;
    private BigDecimal suggestedScore; // AI/初筛建议分
    private BigDecimal maxScore; // 本题满分
    private BigDecimal baseScore; // 卷面基础分
    private BigDecimal answerCoverageRatio;
    private BigDecimal confidence;
    private String status;
    private BigDecimal finalScore; // 教师人工确认后的最终分，覆盖建议分
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
