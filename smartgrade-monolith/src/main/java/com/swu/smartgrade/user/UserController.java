package com.swu.smartgrade.user;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.auth.CurrentUser;
import com.swu.smartgrade.common.auth.RequireRole;
import com.swu.smartgrade.common.auth.Role;
import com.swu.smartgrade.user.dto.LoginRequest;
import com.swu.smartgrade.user.dto.LoginResponse;
import com.swu.smartgrade.user.dto.RegisterRequest;
import com.swu.smartgrade.user.dto.UserView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户服务对外的 HTTP 接口（单体版：/api/auth/**）。
 */
@RestController
@RequestMapping("/api/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me() {
        return ApiResponse.ok(userService.currentUser());
    }

    @GetMapping("/students")
    @RequireRole({Role.TEACHER, Role.ADMIN})
    public ApiResponse<List<UserView>> students(CurrentUser currentUser) {
        return ApiResponse.ok(userService.listStudents(currentUser.classId()).stream()
                .map(userService::toView).toList());
    }
}
