package com.swu.smartgrade.user.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ProfileResponse(
        Long profileId,
        Long teacherId,
        String styleTag,
        String tone,
        String lengthMode,
        String openingStyle,
        List<String> commonPhrases,
        List<String> bannedPhrases,
        Integer profileVersion,
        OffsetDateTime updatedAt
) {
}
