package com.swu.smartgrade.user.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.user.api.dto.ProfileRequest;
import com.swu.smartgrade.user.api.dto.ProfileResponse;
import com.swu.smartgrade.user.application.CommentProfileApplicationService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A4/C01 接口入口：读取和修改当前教师的评语语气、篇幅、开头方式及常用/禁用表达。
 */
@RestController
@RequestMapping("/api/teachers/me/comment-profile")
@Tag(name = "C01 身份与教师偏好服务")
public class TeacherCommentProfileController {
    private final CommentProfileApplicationService service;

    public TeacherCommentProfileController(CommentProfileApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(operationId = "C01-4-01", summary = "读取5类教师评语偏好")
    public ApiResponse<ProfileResponse> get() {
        return ApiResponse.ok(service.getFor(AuthPrincipal.required()), TraceId.current());
    }

    @PutMapping
    @Operation(operationId = "C01-4-02", summary = "维护5类教师评语偏好")
    public ApiResponse<ProfileResponse> save(@Valid @RequestBody ProfileRequest request) {
        return ApiResponse.ok(service.saveFor(AuthPrincipal.required(), request), TraceId.current());
    }
}
