package com.swu.smartgrade.grading.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "hit_evidence")
public class HitEvidence {
    @Id private String evidenceId;
    private String resultId;
    private String pointId;
    private String pointName;
    private String matchType;
    @Column(columnDefinition = "TEXT") private String sentence;
    private Integer startOffset;
    private Integer endOffset;
    private BigDecimal confidence;
}
