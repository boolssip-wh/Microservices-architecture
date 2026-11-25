package com.swu.smartgrade.review.api.dto;

import java.time.OffsetDateTime;

public record ReviewStatusResponse(
        String reviewId,
        String status,
        OffsetDateTime updatedAt
) {
}
