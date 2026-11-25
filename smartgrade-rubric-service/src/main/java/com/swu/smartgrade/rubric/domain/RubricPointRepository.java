package com.swu.smartgrade.rubric.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RubricPointRepository extends JpaRepository<RubricPoint, String> {
    List<RubricPoint> findByRubricIdOrderByPointCode(String rubricId);

    void deleteByRubricId(String rubricId);
}
