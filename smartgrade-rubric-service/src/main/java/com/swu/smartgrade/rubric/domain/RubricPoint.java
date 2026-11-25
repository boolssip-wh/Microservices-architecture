package com.swu.smartgrade.rubric.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "rubric_point")
public class RubricPoint {
    @Id
    private String pointId;
    @Column(nullable = false)
    private String rubricId;
    @Column(nullable = false)
    private String pointCode;
    @Column(nullable = false)
    private String pointName;
    @Column(nullable = false)
    private String pointType;
    @Column(nullable = false)
    private BigDecimal weight;
    @Column(nullable = false)
    private BigDecimal maxScore;
    @Column(columnDefinition = "TEXT")
    private String keywords;

    public String getPointId() {
        return pointId;
    }

    public void setPointId(String pointId) {
        this.pointId = pointId;
    }

    public String getRubricId() {
        return rubricId;
    }

    public void setRubricId(String rubricId) {
        this.rubricId = rubricId;
    }

    public String getPointCode() {
        return pointCode;
    }

    public void setPointCode(String pointCode) {
        this.pointCode = pointCode;
    }

    public String getPointName() {
        return pointName;
    }

    public void setPointName(String pointName) {
        this.pointName = pointName;
    }

    public String getPointType() {
        return pointType;
    }

    public void setPointType(String pointType) {
        this.pointType = pointType;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
}
