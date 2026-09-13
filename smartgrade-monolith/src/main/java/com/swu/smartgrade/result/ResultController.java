package com.swu.smartgrade.result;

import com.swu.smartgrade.assignment.dto.SubmissionDetailView;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.common.auth.RequireRole;
import com.swu.smartgrade.common.auth.Role;
import com.swu.smartgrade.result.dto.ReviewRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/submissions")
public class ResultController {

    private final ResultService resultService;

    public ResultController(ResultService resultService) {
        this.resultService = resultService;
    }

    /** 教师复核改分（自动化评分 + 人工兜底） */
    @PostMapping("/{submissionId}/review")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    public ApiResponse<SubmissionDetailView> review(@PathVariable Long submissionId,
                                                    @Valid @RequestBody ReviewRequest request,
                                                    CurrentUser currentUser) {
        return ApiResponse.ok(resultService.review(submissionId, request.questionId(), request.score(),
                request.reason(), currentUser));
    }
}
