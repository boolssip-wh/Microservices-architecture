package com.swu.smartgrade.review.api.dto;

import java.util.List;

public record CommentSuggestionResponse(
        List<String> suggestedComments,
        List<String> evidenceRefs,
        String styleTag,
        Integer profileVersion
) {
}
