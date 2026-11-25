package com.swu.smartgrade.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Profile("!infra")
public class LocalFileObjectStorage implements ObjectStorage {
    private final Path root;

    public LocalFileObjectStorage(@Value("${app.storage.root:./.data/objects}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    private Path resolve(String key) {
        Path path = root.resolve(key).toAbsolutePath().normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("Object key escapes storage root");
        }
        return path;
    }

    @Override
    public String store(String key, byte[] bytes, String contentType) {
        try {
            Path target = resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            return target.toString();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Object storage write failed", ex);
        }
    }

    @Override
    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Object storage read failed", ex);
        }
    }
}
