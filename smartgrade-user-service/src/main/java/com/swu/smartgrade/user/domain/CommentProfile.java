package com.swu.smartgrade.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_comment_profile")
public class CommentProfile {
    @Id
    private Long profileId;

    @Column(nullable = false, unique = true)
    private Long teacherId;

    @Column(nullable = false)
    private String tone;

    @Column(nullable = false)
    private String lengthMode;

    @Column(nullable = false)
    private String openingStyle;

    @Column(columnDefinition = "TEXT")
    private String commonPhrases;

    @Column(columnDefinition = "TEXT")
    private String bannedPhrases;

    @Column(nullable = false)
    private Integer profileVersion;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public String getLengthMode() {
        return lengthMode;
    }

    public void setLengthMode(String lengthMode) {
        this.lengthMode = lengthMode;
    }

    public String getOpeningStyle() {
        return openingStyle;
    }

    public void setOpeningStyle(String openingStyle) {
        this.openingStyle = openingStyle;
    }

    public String getCommonPhrases() {
        return commonPhrases;
    }

    public void setCommonPhrases(String commonPhrases) {
        this.commonPhrases = commonPhrases;
    }

    public String getBannedPhrases() {
        return bannedPhrases;
    }

    public void setBannedPhrases(String bannedPhrases) {
        this.bannedPhrases = bannedPhrases;
    }

    public Integer getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(Integer profileVersion) {
        this.profileVersion = profileVersion;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
