package com.swu.smartgrade.rubric.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record RubricVersionView(
        String rubricId,
        String batchId,
        int rubricVersion,
        String checksum,
        OffsetDateTime compiledAt,
        List<RubricPointView> points,
        java.math.BigDecimal baseScore,
        java.math.BigDecimal maxScore
) {
    public RubricVersionView(String rubricId, String batchId, int rubricVersion, String checksum,
                             OffsetDateTime compiledAt, List<RubricPointView> points) {
        this(rubricId, batchId, rubricVersion, checksum, compiledAt, points, null, null);
    }
}
