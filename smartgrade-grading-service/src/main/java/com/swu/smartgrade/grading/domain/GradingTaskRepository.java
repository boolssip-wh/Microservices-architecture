package com.swu.smartgrade.grading.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GradingTaskRepository extends JpaRepository<GradingTask, String> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from GradingTask t where t.taskId = :taskId")
    java.util.Optional<GradingTask> findForUpdate(@org.springframework.data.repository.query.Param("taskId") String taskId);
}
