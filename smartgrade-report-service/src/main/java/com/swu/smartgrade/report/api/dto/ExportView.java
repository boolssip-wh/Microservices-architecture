package com.swu.smartgrade.report.api.dto;

import java.time.OffsetDateTime;

public record ExportView(
        String exportId,
        String status,
        String downloadUrl,
        OffsetDateTime expiresAt,
        String auditId
) {
}
