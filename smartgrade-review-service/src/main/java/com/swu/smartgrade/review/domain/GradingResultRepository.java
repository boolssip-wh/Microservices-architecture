package com.swu.smartgrade.review.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GradingResultRepository extends JpaRepository<GradingResult, String> {
    Optional<GradingResult> findByAnswerId(String answerId);

    List<GradingResult> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<GradingResult> findByBatchId(String batchId);
}
