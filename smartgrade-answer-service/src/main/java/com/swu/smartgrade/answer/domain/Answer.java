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
 * A5 答卷实体：保存来源、对象地址、OCR 证据和初筛状态。
 * importJobId + studentNo 在迁移脚本中建立唯一约束，支撑 100+ 份答卷的幂等导入。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "answer")
public class Answer {
    @Id
    private String answerId; // 业务主键 VARCHAR(64)，贯穿 answer/grading/review 三个服务
    private String batchId; // 逻辑外键 -> batch.batch_id
    private String importJobId; // 逻辑外键 -> import_job.job_id
    private String studentNo; // 学号，与 importJobId 组成唯一约束
    private String sourceType; // IMAGE/TEXT，图片走视觉识别，文本走直接匹配
    private String objectKey; // 原始答卷文件在本机对象目录中的相对路径
    private String contentHash;
    @Column(columnDefinition = "TEXT")
    private String ocrText;
    private BigDecimal ocrConfidence; // 识别置信度，DECIMAL(5,2)
    private BigDecimal ocrCoverageRatio; // 手写内容占作答区域比例，用于卷面覆盖率要点
    private Integer ocrCharacterCount; // 识别出的有效字符数，满足最少字数阈值判断
    private String status; // PENDING/RUNNING/WAITING_REVIEW/FAILED 等
    private LocalDateTime createdAt;
}
