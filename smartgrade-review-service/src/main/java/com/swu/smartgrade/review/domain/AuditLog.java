package com.swu.smartgrade.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A5 审计实体：记录操作者、动作、对象和结果摘要，支持“谁在何时改了哪份成绩”的追溯。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog {
    @Id
    private String auditId;
    private Long actorId; // 教师 ID；与 user-service 通过业务 ID 逻辑关联
    private String action; // 例如 FINALIZE_SCORE
    private String objectType; // 例如 RESULT
    private String objectId; // 被操作业务对象 ID
    @Column(columnDefinition = "TEXT")
    private String summaryJson;
    private LocalDateTime createdAt;
}
