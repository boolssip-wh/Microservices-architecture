package com.swu.smartgrade.answer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "import_job")
public class ImportJob {
    @Id
    private String jobId;
    private String batchId;
    private String idempotencyKey;
    @Column(columnDefinition = "TEXT")
    private String rubricSnapshotJson;
    private String sourceType;
    private String numberingRule;
    private String status;
    private Integer acceptedCount;
    private Integer rejectedCount;
    @Column(columnDefinition = "TEXT")
    private String validationJson;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
