package com.swu.smartgrade.report.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateExportRequest(
        @NotBlank(message = "批次编号不能为空") String batchId,
        @NotBlank(message = "导出格式不能为空")
        @Pattern(regexp = "CSV|XLSX", message = "只支持 CSV 或 XLSX") String format,
        String questionNo
) {
}
