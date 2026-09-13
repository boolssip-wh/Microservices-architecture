package com.swu.smartgrade.assignment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByClassIdOrderByIdDesc(Long classId);

    List<Assignment> findByTeacherIdOrderByIdDesc(Long teacherId);

    List<Assignment> findAllByOrderByIdDesc();
}
