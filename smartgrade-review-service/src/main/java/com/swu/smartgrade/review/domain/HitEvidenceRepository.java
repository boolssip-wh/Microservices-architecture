package com.swu.smartgrade.review.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HitEvidenceRepository extends JpaRepository<HitEvidence, String> {
    List<HitEvidence> findByResultId(String resultId);
}
