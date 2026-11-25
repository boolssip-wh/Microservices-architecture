package com.swu.smartgrade.answer.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImportJobRepository extends JpaRepository<ImportJob, String> {
    List<ImportJob> findByStatus(String status);
    Optional<ImportJob> findByBatchIdAndIdempotencyKey(String batchId, String idempotencyKey);
    List<ImportJob> findByBatchIdOrderByCreatedAtDesc(String batchId);
}
