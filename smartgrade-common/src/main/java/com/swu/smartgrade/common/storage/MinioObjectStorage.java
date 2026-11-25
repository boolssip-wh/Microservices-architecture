package com.swu.smartgrade.common.storage;

import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.io.ByteArrayInputStream;

@Component
@Profile("infra")
public class MinioObjectStorage implements ObjectStorage {
    private final MinioClient client;
    private final String bucket;
    private volatile boolean bucketReady;

    public MinioObjectStorage(
            @Value("${MINIO_ENDPOINT:http://127.0.0.1:9000}") String endpoint,
            @Value("${MINIO_ACCESS_KEY:smartgrade}") String accessKey,
            @Value("${MINIO_SECRET_KEY:smartgrade123}") String secretKey,
            @Value("${MINIO_BUCKET:smartgrade}") String bucket) {
        this.client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        this.bucket = bucket;
    }

    private synchronized void ensureBucket() throws Exception {
        if (!bucketReady) {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                try {
                    client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                } catch (io.minio.errors.ErrorResponseException ex) {
                    if (!"BucketAlreadyOwnedByYou".equals(ex.errorResponse().code())) throw ex;
                }
            }
            bucketReady = true;
        }
    }

    private String validKey(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.contains("\\")
                || key.contains("..") || key.contains(":")) {
            throw new IllegalArgumentException("Invalid object key");
        }
        return key;
    }

    @Override
    public String store(String key, byte[] bytes, String contentType) {
        validKey(key);
        try {
            ensureBucket();
            client.putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType).build());
            return key;
        } catch (Exception ex) {
            throw new IllegalStateException("MinIO object write failed", ex);
        }
    }

    @Override
    public byte[] read(String key) {
        validKey(key);
        try (var stream = client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())) {
            return stream.readAllBytes();
        } catch (Exception ex) {
            throw new IllegalStateException("MinIO object read failed", ex);
        }
    }
}
