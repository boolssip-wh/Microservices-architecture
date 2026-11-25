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
        ServerWebExchange external = stripUntrustedHeaders(exchange);
        String path = external.getRequest().getURI().getPath();
        if (!path.startsWith("/api/") || "/api/auth/login".equals(path)) {
            return chain.filter(withTraceId(external));
        }

        String authorization = external.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return unauthorized(external, "请先登录");
        }

        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(authorization.substring(7)).getPayload();
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
