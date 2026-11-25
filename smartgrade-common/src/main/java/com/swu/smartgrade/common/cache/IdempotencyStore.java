package com.swu.smartgrade.common.cache;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {
    boolean putIfAbsent(String key, String value, Duration ttl);
    Optional<String> get(String key);
    void delete(String key);
}
