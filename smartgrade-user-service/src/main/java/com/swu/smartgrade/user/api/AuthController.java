package com.swu.smartgrade.user.api;

import com.swu.smartgrade.common.api.ApiResponse;
import com.swu.smartgrade.common.api.TraceId;
import com.swu.smartgrade.common.security.AuthPrincipal;
import com.swu.smartgrade.user.api.dto.CurrentUserResponse;
import com.swu.smartgrade.user.api.dto.LoginRequest;
import com.swu.smartgrade.user.api.dto.LoginResponse;
import com.swu.smartgrade.user.application.AuthApplicationService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "C01 身份与教师偏好服务")
public class AuthController {
    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/login")
    @Operation(operationId = "C01-1-01", summary = "3 秒内完成教师登录")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authApplicationService.login(request), TraceId.current());
    }

    @GetMapping("/me")
    @Operation(operationId = "C01-2-01", summary = "1 次令牌识别当前身份")
    public ApiResponse<CurrentUserResponse> me(@RequestHeader("Authorization") String authorization) {
        return ApiResponse.ok(authApplicationService.me(AuthPrincipal.required().userId(), authorization), TraceId.current());
    }
}
