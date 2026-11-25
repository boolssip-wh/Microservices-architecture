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
@Table(name = "answer")
public class Answer {
    @Id
    private String answerId;
    private String batchId;
    private String importJobId;
    private String studentNo;
    private String sourceType;
    private String objectKey;
    private String contentHash;
    @Column(columnDefinition = "TEXT")
    private String ocrText;
    private BigDecimal ocrConfidence;
    private BigDecimal ocrCoverageRatio;
    private Integer ocrCharacterCount;
    private String status;
    private LocalDateTime createdAt;
}
