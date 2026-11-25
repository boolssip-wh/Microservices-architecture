package com.swu.smartgrade.grading.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "grading_result")
public class GradingResult {
    @Id private String resultId;
    @Column(unique = true) private String taskId;
    private String answerId;
    private Integer rubricVersion;
    private BigDecimal suggestedScore;
    private BigDecimal baseScore;
    private BigDecimal confidence;
    private String status;
    private LocalDateTime createdAt;
}
