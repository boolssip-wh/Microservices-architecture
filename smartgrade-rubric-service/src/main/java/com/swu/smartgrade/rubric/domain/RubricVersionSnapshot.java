package com.swu.smartgrade.rubric.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "rubric_version_snapshot")
public class RubricVersionSnapshot {
    @Id
    private String snapshotId;
    @Column(nullable = false)
    private String rubricId;
    @Column(nullable = false)
    private Integer version;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;
    @Column(nullable = false)
    private String checksum;
    @Column(nullable = false)
    private LocalDateTime compiledAt;

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getRubricId() {
        return rubricId;
    }

    public void setRubricId(String rubricId) {
        this.rubricId = rubricId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public LocalDateTime getCompiledAt() {
        return compiledAt;
    }

    public void setCompiledAt(LocalDateTime compiledAt) {
        this.compiledAt = compiledAt;
    }
}
