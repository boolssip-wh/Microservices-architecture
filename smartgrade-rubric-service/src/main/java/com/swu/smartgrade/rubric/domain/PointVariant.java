package com.swu.smartgrade.rubric.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "point_variant")
public class PointVariant {
    @Id
    private String variantId;
    @Column(nullable = false)
    private String pointId;
    @Column(nullable = false)
    private String variantText;
    @Column(nullable = false)
    private String matchType;
    @Column(nullable = false)
    private BigDecimal confidence;
    @Column(nullable = false)
    private Boolean enabled;

    public String getVariantId() {
        return variantId;
    }

    public void setVariantId(String variantId) {
        this.variantId = variantId;
    }

    public String getPointId() {
        return pointId;
    }

    public void setPointId(String pointId) {
        this.pointId = pointId;
    }

    public String getVariantText() {
        return variantText;
    }

    public void setVariantText(String variantText) {
        this.variantText = variantText;
    }

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
