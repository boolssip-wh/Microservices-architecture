package com.swu.smartgrade.report.domain;

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
@Table(name = "export_record")
public class ExportRecord {
    @Id
    private String exportId;
    private String batchId;
    private Long teacherId;
    private String format;
    private String fileKey;
    private String status;
    private String auditId;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
