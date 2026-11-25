package com.swu.smartgrade.assignment;

import com.swu.smartgrade.assignment.dto.AssignmentView;
import com.swu.smartgrade.assignment.dto.CreateAssignmentRequest;
import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.common.auth.RequireRole;
import com.swu.smartgrade.common.auth.Role;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;

    public AssignmentController(AssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /** 教师发布作业 */
    @PostMapping
    @RequireRole({Role.TEACHER, Role.ADMIN})
    public ApiResponse<Long> create(@Valid @RequestBody CreateAssignmentRequest request, CurrentUser currentUser) {
        return ApiResponse.ok(assignmentService.create(request, currentUser));
    }

    /** 作业列表：学生看本班，教师看自己发布的，管理员看全部 */
    @GetMapping
    public ApiResponse<List<AssignmentView>> list(CurrentUser currentUser) {
        return ApiResponse.ok(assignmentService.list(currentUser));
    }

    @GetMapping("/{id}")
    public ApiResponse<AssignmentView> detail(@PathVariable Long id, CurrentUser currentUser) {
        return ApiResponse.ok(assignmentService.detail(id, currentUser));
    }
}
