package com.swu.smartgrade.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    List<Submission> findByAssignmentIdOrderByStudentId(Long assignmentId);

    List<Submission> findByStudentIdOrderByIdDesc(Long studentId);

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    Optional<Submission> findBySubmitKey(String submitKey);

    long countByAssignmentId(Long assignmentId);
}
