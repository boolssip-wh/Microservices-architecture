package com.swu.smartgrade.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "review_record")
public class ReviewRecord {
    @Id
    private String reviewId;
    @Column(unique = true)
    private String resultId;
    private Long teacherId;
    private BigDecimal finalScore;
    private String reviewStatus;
    @Column(columnDefinition = "TEXT")
    private String reviewNote;
    private LocalDateTime confirmedAt;
}
