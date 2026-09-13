package com.swu.smartgrade.report;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.auth.RequireRole;
import com.swu.smartgrade.common.auth.Role;
import com.swu.smartgrade.report.dto.AssignmentReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** 班级看板：分数分布 + 错题排行 + 知识点薄弱分析 */
    @GetMapping("/assignments/{assignmentId}")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    public ApiResponse<AssignmentReport> assignmentReport(@PathVariable Long assignmentId) {
        return ApiResponse.ok(reportService.buildAssignmentReport(assignmentId));
    }
}
