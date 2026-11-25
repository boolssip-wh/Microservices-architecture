package com.swu.smartgrade.review.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, String> {
    Optional<ReviewRecord> findByResultId(String resultId);
}
