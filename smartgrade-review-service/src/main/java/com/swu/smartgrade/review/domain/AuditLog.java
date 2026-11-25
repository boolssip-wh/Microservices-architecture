package com.swu.smartgrade.review.domain;

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
@Table(name = "audit_log")
public class AuditLog {
    @Id
    private String auditId;
    private Long actorId;
    private String action;
    private String objectType;
    private String objectId;
    @Column(columnDefinition = "TEXT")
    private String summaryJson;
    private LocalDateTime createdAt;
}
