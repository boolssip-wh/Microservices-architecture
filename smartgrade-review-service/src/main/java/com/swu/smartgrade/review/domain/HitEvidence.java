package com.swu.smartgrade.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A5 可解释证据投影：记录命中的要点、原句、起止位置和置信度。
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "review_evidence_projection")
public class HitEvidence {
    @Id
    private String evidenceId;
    private String resultId;
    private String pointId;
    private String pointName; // 展示给教师看的评分要点名称
    private String matchType; // EXACT/VARIANT/LENGTH 等匹配方式
    @Column(columnDefinition = "TEXT")
    private String sentence;
    private Integer startOffset; // 证据原句起始下标，前端可高亮原文
    private Integer endOffset; // 证据原句结束下标
    private BigDecimal confidence; // 该要点命中的置信度
}
