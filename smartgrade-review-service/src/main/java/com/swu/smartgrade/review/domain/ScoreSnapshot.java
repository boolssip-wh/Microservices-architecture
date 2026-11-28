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
 * A5 成绩快照：教师确认后保存不可变的最终结果和评语 JSON，供报告与追溯使用。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "score_snapshot")
public class ScoreSnapshot {
    @Id
    private String snapshotId;
    private String reviewId; // 逻辑外键 -> review_record.review_id
    private BigDecimal finalScore;
    @Column(columnDefinition = "TEXT")
    private String snapshotJson;
    private LocalDateTime createdAt;
}
