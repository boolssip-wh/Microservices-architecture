package com.swu.smartgrade.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerItemRepository extends JpaRepository<AnswerItem, Long> {

    List<AnswerItem> findBySubmissionIdOrderByOrderNo(Long submissionId);

    List<AnswerItem> findBySubmissionIdIn(List<Long> submissionIds);

    long countByScoreSource(String scoreSource);
}
