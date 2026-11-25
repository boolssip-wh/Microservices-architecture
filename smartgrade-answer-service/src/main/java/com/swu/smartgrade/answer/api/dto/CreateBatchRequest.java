package com.swu.smartgrade.answer.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;

public record CreateBatchRequest(
        @NotBlank(message = "考试名称不能为空") @Size(min=2, max=100) String examName,
        @NotBlank(message = "题号不能为空") @Pattern(regexp="[A-Za-z0-9_-]{1,20}", message="题号需为1–20位字母、数字、下划线或短横线") String questionNo,
        @NotBlank(message = "题目标题不能为空") @Size(min=2, max=200) String questionTitle,
        String questionText,
        @NotNull(message = "满分不能为空") @DecimalMin(value = "0.01") @DecimalMax("100") @Digits(integer=3, fraction=2) BigDecimal maxScore,
        @DecimalMin(value = "0", message = "基础分不能为负数") @Digits(integer=3, fraction=2) BigDecimal baseScore
) {
    public CreateBatchRequest(String examName, String questionNo, String questionTitle,
                              String questionText, BigDecimal maxScore) {
        this(examName, questionNo, questionTitle, questionText, maxScore, null);
    }
    public CreateBatchRequest {
        examName = examName == null ? null : examName.trim();
        questionNo = questionNo == null ? null : questionNo.trim();
        questionTitle = questionTitle == null ? null : questionTitle.trim();
    }
}
