package com.swu.smartgrade.common.auth;

import com.swu.smartgrade.common.api.BusinessException;

/**
 * 请求级登录上下文。单体版用 ThreadLocal 保存；拆分微服务后可换成网关透传的请求头。
 */
public final class AuthContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    public static CurrentUser require() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw new BusinessException(401, "未登录或登录状态已过期");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
