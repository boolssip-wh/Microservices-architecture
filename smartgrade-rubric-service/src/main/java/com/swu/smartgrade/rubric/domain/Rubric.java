package com.swu.smartgrade.rubric.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "rubric")
public class Rubric {
    @Id
    private String rubricId;
    @Column(nullable = false)
    private String batchId;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String sourceText;
    @Column(nullable = false)
    private BigDecimal maxScore;
    @Column(nullable = false)
    private BigDecimal baseScore;
    @Column(nullable = false)
    private Integer version;
    @Column(nullable = false)
    private String status;
    private String checksum;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime compiledAt;

    public String getRubricId() {
        return rubricId;
    }

    public void setRubricId(String rubricId) {
        this.rubricId = rubricId;
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public BigDecimal getBaseScore() { return baseScore; }
    public void setBaseScore(BigDecimal baseScore) { this.baseScore = baseScore; }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompiledAt() {
        return compiledAt;
    }

    public void setCompiledAt(LocalDateTime compiledAt) {
        this.compiledAt = compiledAt;
    }
}
