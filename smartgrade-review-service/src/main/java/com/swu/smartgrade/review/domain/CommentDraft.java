package com.swu.smartgrade.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "comment_draft")
public class CommentDraft {
    @Id
    private String commentId;
    private String reviewId;
    private Long teacherId;
    private Long profileId;
    private Integer profileVersion;
    @Column(columnDefinition = "TEXT")
    private String suggestedText;
    @Column(columnDefinition = "TEXT")
    private String finalText;
    private String status;
    private LocalDateTime createdAt;
}
