package com.swu.smartgrade.result;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewLogRepository extends JpaRepository<ReviewLog, Long> {

    List<ReviewLog> findBySubmissionIdOrderByIdAsc(Long submissionId);
}
