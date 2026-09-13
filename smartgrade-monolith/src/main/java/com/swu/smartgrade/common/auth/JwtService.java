package com.swu.smartgrade.common.auth;

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

/**
 * 本地 HMAC 令牌：签发与校验。第 6 周接入网关后，改为网关统一校验并向后透传用户信息。
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.ttl-hours:8}") long ttlHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = Duration.ofHours(ttlHours).toMillis();
    }

    public String issue(Long userId, String username, String realName, Role role, Long classId) {
        var builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("realName", realName)
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis));
        if (classId != null) {
            builder.claim("classId", String.valueOf(classId));
        }
        return builder.signWith(key).compact();
    }

    public CurrentUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String classIdClaim = claims.get("classId", String.class);
            return new CurrentUser(
                    Long.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("realName", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    classIdClaim == null ? null : Long.valueOf(classIdClaim));
        } catch (Exception ex) {
            throw new BusinessException(401, "登录状态无效，请重新登录");
        }
    }
}
