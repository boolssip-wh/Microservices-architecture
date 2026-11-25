package com.swu.smartgrade.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ProfileRequest(
        @NotBlank @Pattern(regexp = "NEUTRAL|ENCOURAGING|STRICT") String tone,
        @NotBlank @Pattern(regexp = "SHORT|MEDIUM|LONG") String lengthMode,
        @NotBlank @Pattern(regexp = "DIRECT|PRAISE_FIRST|QUESTION") String openingStyle,
        @Size(max = 10) List<@NotBlank @Size(min = 2, max = 40) String> commonPhrases,
        @Size(max = 20) List<@NotBlank @Size(min = 1, max = 20) String> bannedPhrases
) {
    public ProfileRequest {
        commonPhrases = commonPhrases == null ? List.of() : commonPhrases.stream().distinct().toList();
        bannedPhrases = bannedPhrases == null ? List.of() : bannedPhrases.stream().distinct().toList();
    }
}
