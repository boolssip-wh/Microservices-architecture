package com.swu.smartgrade.answer.api.dto;

import java.util.List;

public record ValidationView(
        List<String> duplicates,
        List<String> blank,
        List<String> corrupted,
        String status
) {
}
