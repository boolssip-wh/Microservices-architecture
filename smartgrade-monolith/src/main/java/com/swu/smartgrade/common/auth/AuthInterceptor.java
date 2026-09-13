package com.swu.smartgrade.common.auth;

import com.swu.smartgrade.common.api.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 拦截 /api/** ：先解析令牌，再按 {@link RequireRole} 做角色校验。
 */
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 先清理上一个请求可能遗留的上下文：
        // preHandle 抛异常时 afterCompletion 不会执行，若不清理会让后续请求"继承"上一个用户。
        AuthContext.clear();

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            AuthContext.set(jwtService.parse(header.substring(BEARER_PREFIX.length())));
        }

        CurrentUser current = AuthContext.get();
        if (current == null) {
            throw new BusinessException(401, "请先登录");
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            return true;
        }

        boolean allowed = Arrays.stream(requireRole.value()).anyMatch(role -> role == current.role());
        if (!allowed) {
            throw new BusinessException(403, "当前角色（" + current.role() + "）无权访问该接口");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
