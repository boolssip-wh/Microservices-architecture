package com.swu.smartgrade.rubric.api.dto;

import java.time.OffsetDateTime;

public record CompileRubricResponse(
        String rubricId,
        int rubricVersion,
        String checksum,
        OffsetDateTime compiledAt
) {
}
