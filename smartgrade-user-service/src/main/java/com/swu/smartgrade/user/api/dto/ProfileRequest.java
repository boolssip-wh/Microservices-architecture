package com.swu.smartgrade.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ProfileRequest(
        // 输入限制：语气只允许 NEUTRAL/ENCOURAGING/STRICT。
        @NotBlank @Pattern(regexp = "NEUTRAL|ENCOURAGING|STRICT") String tone,
        // 输入限制：长度只允许 SHORT/MEDIUM/LONG。
        @NotBlank @Pattern(regexp = "SHORT|MEDIUM|LONG") String lengthMode,
        // 输入限制：开头方式只允许 DIRECT/PRAISE_FIRST/QUESTION。
        @NotBlank @Pattern(regexp = "DIRECT|PRAISE_FIRST|QUESTION") String openingStyle,
        // 输入限制：常用表达最多 10 条，每条 2-40 字。
        @Size(max = 10) List<@NotBlank @Size(min = 2, max = 40) String> commonPhrases,
        // 输入限制：禁用表达最多 20 条，每条 1-20 字。
        @Size(max = 20) List<@NotBlank @Size(min = 1, max = 20) String> bannedPhrases
) {
    public ProfileRequest {
        commonPhrases = commonPhrases == null ? List.of() : commonPhrases.stream().distinct().toList();
        bannedPhrases = bannedPhrases == null ? List.of() : bannedPhrases.stream().distinct().toList();
    }
}
