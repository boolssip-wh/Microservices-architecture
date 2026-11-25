package com.swu.smartgrade.common.cache;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Profile("!infra")
public class LocalIdempotencyStore implements IdempotencyStore {
    private record Entry(String value, Instant expires) {}
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    public boolean putIfAbsent(String key, String value, Duration ttl) {
        AtomicBoolean inserted = new AtomicBoolean();
        entries.compute(key, (ignored, old) -> {
            if (old == null || !old.expires().isAfter(Instant.now())) {
                inserted.set(true);
                return new Entry(value, Instant.now().plus(ttl));
            }
            return old;
        });
        return inserted.get();
    }

    public Optional<String> get(String key) {
        Entry entry = entries.computeIfPresent(key,
                (ignored, old) -> old.expires().isAfter(Instant.now()) ? old : null);
        return Optional.ofNullable(entry).map(Entry::value);
    }

    public void delete(String key) { entries.remove(key); }
}
