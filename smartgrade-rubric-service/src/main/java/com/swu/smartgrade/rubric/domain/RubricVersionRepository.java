package com.swu.smartgrade.rubric.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RubricVersionRepository extends JpaRepository<RubricVersionSnapshot, String> {
    Optional<RubricVersionSnapshot> findByRubricIdAndVersion(String rubricId, Integer version);
}
