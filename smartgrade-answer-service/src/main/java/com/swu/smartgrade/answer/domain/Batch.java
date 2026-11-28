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

/**
 * A5 业务实体：一次作业批次。teacherId 是 user-service 的逻辑外键，不跨库建物理外键。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "batch")
public class Batch {
    @Id
    private String batchId; // 业务主键 VARCHAR(64)，由应用生成，避免依赖数据库自增跨服务不可控
    private Long teacherId; // 逻辑外键 -> smartgrade-user-service.teacher.teacher_id
    private String examName;
    private String questionNo; // 题目序号，可为 Q1/Q2，不再固定写死为 Q1
    private String questionTitle;
    @Column(columnDefinition = "TEXT")
    private String questionText;
    private BigDecimal maxScore; // DECIMAL(10,2)：两位小数，适合 20.00 分这类评分数据
    private BigDecimal baseScore; // 基础分，用于空答/未命中要点仍保留最低分
    private String status; // CREATED/PROCESSING/REVIEWING/COMPLETED/FAILED
    private LocalDateTime createdAt;
}
