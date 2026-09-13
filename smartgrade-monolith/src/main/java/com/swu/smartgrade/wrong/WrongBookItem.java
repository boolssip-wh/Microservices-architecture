package com.swu.smartgrade.wrong;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 错题本条目（错题 / 推荐服务）：同一道题反复错时累加次数。
 */
@Entity
@Table(name = "wrong_book_item")
@Getter
@Setter
public class WrongBookItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "submission_id")
    private Long submissionId;

    @Column(name = "knowledge_tag", length = 64)
    private String knowledgeTag;

    @Column(name = "wrong_times", nullable = false)
    private int wrongTimes = 1;

    @Column(name = "last_answer", length = 2000)
    private String lastAnswer;

    @Column(nullable = false)
    private boolean resolved;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}
