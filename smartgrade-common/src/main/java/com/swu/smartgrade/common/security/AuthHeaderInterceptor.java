package com.swu.smartgrade.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import com.swu.smartgrade.common.api.BusinessException;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 业务服务侧安全拦截：/api/** 必须带网关令牌，/internal/** 必须带服务令牌；
 * 身份头由网关统一写入，这里只做格式校验并构造 AuthPrincipal。
 */
@Component
public class AuthHeaderInterceptor implements HandlerInterceptor {
    private final String serviceToken;
    private final String gatewayToken;

    public AuthHeaderInterceptor(@Value("${SERVICE_TOKEN:local-service-token}") String serviceToken,
                                 @Value("${GATEWAY_TOKEN:local-gateway-token}") String gatewayToken) {
        this.serviceToken = serviceToken;
        this.gatewayToken = gatewayToken;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // C1 业务服务二次校验：公开 /api/** 请求必须携带网关写入的令牌，避免绕过 Gateway 直连服务。
        if (request.getRequestURI().startsWith("/api/")) {
            String suppliedGateway = request.getHeader("X-Gateway-Token");
            if (gatewayToken.isBlank() || suppliedGateway == null || !java.security.MessageDigest.isEqual(
                    gatewayToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    suppliedGateway.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                throw BusinessException.unauthorized("请求必须通过统一网关");
            }
        }
        // 内部接口只允许携带服务令牌的服务间调用，前端 JWT 不能直接访问 /internal/**。
        if (request.getRequestURI().startsWith("/internal/")) {
            String supplied = request.getHeader("X-Service-Token");
            if (serviceToken.isBlank() || supplied == null || !java.security.MessageDigest.isEqual(
                    serviceToken.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    supplied.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                throw BusinessException.unauthorized("内部服务凭据无效");
            }
        }
        // 解析网关透传的身份头，并校验 userId 是合法正整数，供 Controller/Service 构造 AuthPrincipal。
        String id = request.getHeader("X-User-Id");
        if (id != null && !id.isBlank()) {
            if (!id.matches("[1-9][0-9]{0,17}")) throw BusinessException.unauthorized("身份编号无效");
            request.setAttribute(AuthPrincipal.ATTR, new AuthPrincipal(
                    Long.parseLong(id),
                    request.getHeader("X-Username"),
                    request.getHeader("X-Real-Name"),
                    request.getHeader("X-Role")
            ));
        }
        return true;
    }
}
