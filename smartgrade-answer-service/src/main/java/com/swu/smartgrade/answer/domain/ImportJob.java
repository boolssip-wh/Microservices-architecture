package com.swu.smartgrade.answer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A5 导入任务实体：idempotencyKey 保证重复上传不重复生成任务，rubricSnapshotJson 固定本次规则版本。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "import_job")
public class ImportJob {
    @Id
    private String jobId;
    private String batchId; // 逻辑外键 -> batch.batch_id
    private String idempotencyKey; // 与 batchId 组合形成唯一约束，防止重复导入
    @Column(columnDefinition = "TEXT")
    private String rubricSnapshotJson;
    private String sourceType;
    private String numberingRule;
    private String status; // PENDING/RUNNING/COMPLETED/PARTIAL_FAILED/FAILED
    private Integer acceptedCount; // 验收后的有效答卷数
    private Integer rejectedCount; // 格式或重复等问题被拒绝的数量
    @Column(columnDefinition = "TEXT")
    private String validationJson;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}
