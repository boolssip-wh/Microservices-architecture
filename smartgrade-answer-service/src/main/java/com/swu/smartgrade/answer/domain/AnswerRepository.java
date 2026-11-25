package com.swu.smartgrade.answer.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnswerRepository extends JpaRepository<Answer, String> {
    List<Answer> findByImportJobIdOrderByStudentNo(String importJobId);

    List<Answer> findByBatchIdOrderByStudentNo(String batchId);

    long countByBatchId(String batchId);

    long countByBatchIdAndStatus(String batchId, String status);
}
