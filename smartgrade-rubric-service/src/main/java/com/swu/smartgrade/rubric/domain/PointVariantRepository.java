package com.swu.smartgrade.rubric.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointVariantRepository extends JpaRepository<PointVariant, String> {
    List<PointVariant> findByPointId(String pointId);
    void deleteByPointId(String pointId);
}
