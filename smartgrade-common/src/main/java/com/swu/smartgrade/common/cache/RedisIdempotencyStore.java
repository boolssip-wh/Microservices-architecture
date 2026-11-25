package com.swu.smartgrade.common.cache;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Optional;

@Component
@Profile("infra")
public class RedisIdempotencyStore implements IdempotencyStore {
    private final StringRedisTemplate redis;
    public RedisIdempotencyStore(StringRedisTemplate redis) { this.redis = redis; }
    public boolean putIfAbsent(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent("smartgrade:" + key, value, ttl));
    }
    public Optional<String> get(String key) {
        return Optional.ofNullable(redis.opsForValue().get("smartgrade:" + key));
    }
    public void delete(String key) { redis.delete("smartgrade:" + key); }
}
