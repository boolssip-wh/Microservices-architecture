package com.swu.smartgrade.bank;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 题目（题库服务）：题干、标准答案、评分规则（keywords）、解析和知识点标签。
 */
@Entity
@Table(name = "question")
@Getter
@Setter
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32)
    private String subject;

    /** 知识点标签：错题本聚类与相似题推荐的依据 */
    @Column(name = "knowledge_tag", length = 64)
    private String knowledgeTag;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private QuestionType type;

    @Column(nullable = false, length = 2000)
    private String stem;

    /** 选项，每行一个：A. xxx */
    @Column(length = 2000)
    private String options;

    /** 标准答案；填空/主观题可用 | 分隔多个可接受答案或关键词 */
    @Column(name = "answer_key", nullable = false, length = 2000)
    private String answerKey;

    /** 主观题关键词规则，| 分隔 */
    @Column(length = 2000)
    private String keywords;

    /** 该题在作业中的默认分值 */
    @Column(name = "default_score", nullable = false)
    private int defaultScore = 10;

    @Column(length = 4000)
    private String analysis;

    /** 难度 1-5 */
    private int difficulty = 3;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
