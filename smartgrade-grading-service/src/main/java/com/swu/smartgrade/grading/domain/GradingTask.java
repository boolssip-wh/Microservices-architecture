package com.swu.smartgrade.grading.domain;

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
@Table(name = "grading_task")
public class GradingTask {
    @Id
    private String taskId;
    private String answerId;
    private String status;
    private Integer retryCount;
    private String errorCode;
    private Integer rubricVersion;
    private String idempotencyKey;
    @Column(columnDefinition = "TEXT")
    private String retryKeys;
    @Column(columnDefinition = "TEXT")
    private String normalizedText;
    private BigDecimal confidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
