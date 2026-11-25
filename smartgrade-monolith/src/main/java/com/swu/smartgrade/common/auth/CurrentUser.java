package com.swu.smartgrade.common.auth;

/**
 * 当前登录用户（由 JWT 解析得到，绑定到本次请求线程）。
 */
public record CurrentUser(Long id, String username, String realName, Role role, Long classId) {

    public boolean isStudent() {
        return role == Role.STUDENT;
    }

    public boolean isTeacher() {
        return role == Role.TEACHER;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
