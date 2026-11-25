package com.swu.smartgrade.common.security;

import com.swu.smartgrade.common.api.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(@Value("${app.jwt.secret:smartgrade-platform-demo-secret-key-2026-change-me}") String secret,
                      @Value("${app.jwt.ttl-hours:8}") long ttlHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = Duration.ofHours(ttlHours).toMillis();
    }

    public String issue(Long userId, String username, String realName, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("realName", realName)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    public AuthPrincipal parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return new AuthPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("realName", String.class),
                    claims.get("role", String.class)
            );
        } catch (Exception ex) {
            throw BusinessException.unauthorized("登录状态无效或已过期");
        }
    }

    public java.time.OffsetDateTime expiresAt(String token) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token)
                    .getPayload().getExpiration().toInstant().atOffset(java.time.ZoneOffset.UTC);
        } catch (Exception ex) {
            throw BusinessException.unauthorized("登录状态无效或已过期");
        }
    }
}
