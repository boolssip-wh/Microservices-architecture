package com.swu.smartgrade.bank.dto;

import com.swu.smartgrade.bank.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionRequest(
        String subject,
        String knowledgeTag,
        @NotNull(message = "题型不能为空") QuestionType type,
        @NotBlank(message = "题干不能为空") String stem,
        String options,
        @NotBlank(message = "标准答案不能为空") String answerKey,
        String keywords,
        Integer defaultScore,
        String analysis,
        Integer difficulty) {
}
