package com.swu.smartgrade.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

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
    private String pointName;
    private String matchType;
    @Column(columnDefinition = "TEXT")
    private String sentence;
    private Integer startOffset;
    private Integer endOffset;
    private BigDecimal confidence;
}
