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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "review_result_projection")
public class GradingResult {
    @Id
    private String resultId;
    @Column(unique = true)
    private String answerId;
    private String batchId;
    private String studentNo;
    @Column(columnDefinition = "TEXT")
    private String answerText;
    private Long teacherId;
    private Integer rubricVersion;
    @Column(columnDefinition = "TEXT")
    private String scoringPointsJson;
    private BigDecimal suggestedScore;
    private BigDecimal maxScore;
    private BigDecimal baseScore;
    private BigDecimal answerCoverageRatio;
    private BigDecimal confidence;
    private String status;
    private BigDecimal finalScore;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
}
