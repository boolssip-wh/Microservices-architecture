package com.swu.smartgrade.grading.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GradingResultRepository extends JpaRepository<GradingResult, String> {
    Optional<GradingResult> findByTaskId(String taskId);
}
