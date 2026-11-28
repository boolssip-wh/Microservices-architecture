package com.swu.smartgrade.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * 统一入口鉴权：先删除外部伪造身份头，再校验 JWT，成功后把 userId/role 透传下游。
 * 无效令牌返回 401 并带统一错误码，不进入业务服务。
 */
@Component
public class GatewayJwtAuthenticationFilter implements GlobalFilter, Ordered {
    private final SecretKey key;
    private final ObjectMapper objectMapper;
    private final String gatewayToken;

    public GatewayJwtAuthenticationFilter(
            @Value("${app.jwt.secret:smartgrade-platform-demo-secret-key-2026-change-me}") String secret,
            @Value("${app.gateway.token:local-gateway-token}") String gatewayToken,
            ObjectMapper objectMapper) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.gatewayToken = gatewayToken;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // C1 第一步：删除客户端可能伪造的身份头，避免未认证请求冒充用户或服务。
        ServerWebExchange external = stripUntrustedHeaders(exchange);
        String path = external.getRequest().getURI().getPath();
        // 登录接口和静态/健康检查请求无需 JWT；其余 /api/** 必须先通过网关认证。
        if (!path.startsWith("/api/") || "/api/auth/login".equals(path)) {
            return chain.filter(withTraceId(external));
        }

        // C1 现场证据：Authorization 缺失或不是 Bearer 格式时直接返回 401。
        String authorization = external.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(external, "请先登录");
        }

        try {
            // 校验 JWT 签名和有效期；失败由下面 catch 统一转成 401。
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
            // C1 身份透传：解析出的 userId、用户名、姓名、角色写回请求头，业务服务不再信任外部原值。
            ServerWebExchange authenticated = withTraceId(external).mutate()
                    .request(builder -> builder.headers(headers -> {
                        headers.set("X-User-Id", claims.getSubject());
                        headers.set("X-Username", claims.get("username", String.class));
                        headers.set("X-Real-Name", claims.get("realName", String.class));
                        headers.set("X-Role", claims.get("role", String.class));
                    }))
                    .build();
            return chain.filter(authenticated);
        } catch (Exception ex) {
            return unauthorized(external, "登录状态无效或已过期");
        }
    }

    private ServerWebExchange withTraceId(ServerWebExchange exchange) {
        // 统一生成 TraceId，响应头用于现场核对请求确实经过 Gateway。
        String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        String finalTraceId = traceId;
        exchange.getResponse().getHeaders().set("X-Trace-Id", traceId);
        exchange.getResponse().getHeaders().set("X-SmartGrade-Gateway", "smartgrade-gateway");
        return exchange.mutate().request(builder -> builder.headers(headers -> {
            headers.set("X-Trace-Id", finalTraceId);
            headers.set("X-Gateway-Token", gatewayToken);
        })).build();
    }

    private ServerWebExchange stripUntrustedHeaders(ServerWebExchange exchange) {
        // 防伪造：网关会重新写入可信身份头，因此先清空外部同名头。
        return exchange.mutate().request(builder -> builder.headers(headers -> {
            headers.remove("X-User-Id");
            headers.remove("X-Username");
            headers.remove("X-Real-Name");
            headers.remove("X-Role");
            headers.remove("X-Gateway-Token");
            headers.remove("X-Service-Token");
        })).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        // C1 无效令牌证据：HTTP 401 + 统一 JSON 错误体，不进入后续业务服务。
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String traceId = UUID.randomUUID().toString().replace("-", "");
        exchange.getResponse().getHeaders().set("X-Trace-Id", traceId);
        exchange.getResponse().getHeaders().set("X-SmartGrade-Gateway", "smartgrade-gateway");
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(Map.of(
                    "code", 40100,
                    "message", message,
                    "traceId", traceId,
                    "timestamp", OffsetDateTime.now().toString()
            ));
            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (Exception ex) {
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
