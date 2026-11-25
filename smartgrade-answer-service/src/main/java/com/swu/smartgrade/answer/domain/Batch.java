package com.swu.smartgrade.answer.domain;

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
@Table(name = "batch")
public class Batch {
    @Id
    private String batchId;
    private Long teacherId;
    private String examName;
    private String questionNo;
    private String questionTitle;
    @Column(columnDefinition = "TEXT")
    private String questionText;
    private BigDecimal maxScore;
    private BigDecimal baseScore;
    private String status;
    private LocalDateTime createdAt;
}
