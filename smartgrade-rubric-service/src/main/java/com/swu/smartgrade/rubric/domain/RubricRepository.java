package com.swu.smartgrade.rubric.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface RubricRepository extends JpaRepository<Rubric, String> {
    Optional<Rubric> findFirstByBatchIdAndStatusOrderByVersionDescCompiledAtDescRubricIdDesc(String batchId, String status);

    @Query("select r.batchId from Rubric r where r.rubricId = :rubricId")
    Optional<String> findBatchId(@Param("rubricId") String rubricId);

    // All compilers for a batch acquire the same rows in the same order before allocating a version.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rubric r where r.batchId = :batchId order by r.rubricId")
    List<Rubric> lockBatch(@Param("batchId") String batchId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Rubric r where r.rubricId = :rubricId")
    Optional<Rubric> findForUpdate(@Param("rubricId") String rubricId);
}
