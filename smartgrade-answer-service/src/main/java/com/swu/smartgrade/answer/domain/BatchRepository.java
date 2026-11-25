package com.swu.smartgrade.answer.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchRepository extends JpaRepository<Batch, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from Batch b where b.batchId = :batchId")
    java.util.Optional<Batch> lockById(@org.springframework.data.repository.query.Param("batchId") String batchId);
    Page<Batch> findByTeacherId(Long teacherId, Pageable pageable);

    Page<Batch> findByTeacherIdAndStatus(Long teacherId, String status, Pageable pageable);
}
