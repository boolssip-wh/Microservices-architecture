package com.swu.smartgrade.common.security;

import com.swu.smartgrade.common.api.BusinessException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public record AuthPrincipal(Long userId, String username, String realName, String role) {
    public static final String ATTR = "smartgrade.principal";

    public static AuthPrincipal required() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object value = attributes == null ? null : attributes.getAttribute(ATTR, RequestAttributes.SCOPE_REQUEST);
        if (!(value instanceof AuthPrincipal principal)) {
            throw BusinessException.unauthorized("请先登录");
        }
        return principal;
    }

    public boolean isTeacher() {
        return "TEACHER".equals(role) || "ADMIN".equals(role);
    }
}
