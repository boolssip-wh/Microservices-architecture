package com.swu.smartgrade.report.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateExportRequest(
        // 输入限制：批次编号不能为空。
        @NotBlank(message = "批次编号不能为空") String batchId,
        // 输入限制：导出格式只能是 CSV 或 XLSX。
        @NotBlank(message = "导出格式不能为空")
        @Pattern(regexp = "CSV|XLSX", message = "只支持 CSV 或 XLSX") String format,
        String questionNo
) {
}
