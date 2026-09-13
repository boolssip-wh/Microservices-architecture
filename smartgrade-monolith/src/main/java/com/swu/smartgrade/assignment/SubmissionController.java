package com.swu.smartgrade.assignment;

import com.swu.smartgrade.assignment.dto.SubmissionDetailView;
import com.swu.smartgrade.assignment.dto.SubmitAnswersRequest;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.common.auth.RequireRole;
import com.swu.smartgrade.common.auth.Role;
import com.swu.smartgrade.result.ResultService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ResultService resultService;

    public SubmissionController(SubmissionService submissionService, ResultService resultService) {
        this.submissionService = submissionService;
        this.resultService = resultService;
    }

    /** 学生提交作业：提交后同步返回判分结果（第 7 周改为「先返回任务号，Kafka 异步批改」） */
    @PostMapping("/assignments/{assignmentId}/submissions")
    @RequireRole({Role.STUDENT})
    public ApiResponse<SubmissionDetailView> submit(@PathVariable Long assignmentId,
                                                    @Valid @RequestBody SubmitAnswersRequest request,
                                                    @RequestHeader(value = "Idempotency-Key", required = false) String submitKey,
                                                    CurrentUser currentUser) {
        Long submissionId = submissionService.submit(assignmentId, request, currentUser, submitKey);
        return ApiResponse.ok(resultService.detail(submissionId, currentUser));
    }

    /** 单次提交的评分明细（学生看自己的，教师/管理员看任意） */
    @GetMapping("/submissions/{id}")
    public ApiResponse<SubmissionDetailView> detail(@PathVariable Long id, CurrentUser currentUser) {
        return ApiResponse.ok(resultService.detail(id, currentUser));
    }

    /** 教师查看某次作业的全部提交 */
    @GetMapping("/assignments/{assignmentId}/submissions")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    public ApiResponse<List<SubmissionDetailView>> list(@PathVariable Long assignmentId, CurrentUser currentUser) {
        return ApiResponse.ok(submissionService.listByAssignment(assignmentId).stream()
                .map(submission -> resultService.detail(submission.getId(), currentUser))
                .toList());
    }

    /** 学生查看自己的提交历史 */
    @GetMapping("/submissions")
    public ApiResponse<List<SubmissionDetailView>> mySubmissions(@RequestParam(required = false) Long assignmentId,
                                                                 CurrentUser currentUser) {
        return ApiResponse.ok(resultService.mySubmissions(currentUser, assignmentId));
    }
}
