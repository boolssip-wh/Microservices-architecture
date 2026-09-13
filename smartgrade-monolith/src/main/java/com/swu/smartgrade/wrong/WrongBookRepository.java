package com.swu.smartgrade.wrong;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WrongBookRepository extends JpaRepository<WrongBookItem, Long> {

    List<WrongBookItem> findByStudentIdOrderByUpdatedAtDesc(Long studentId);

    List<WrongBookItem> findByStudentIdAndResolvedFalseOrderByUpdatedAtDesc(Long studentId);

    Optional<WrongBookItem> findByStudentIdAndQuestionId(Long studentId, Long questionId);

    long countByResolvedFalse();
}
