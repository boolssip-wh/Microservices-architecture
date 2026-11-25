package com.swu.smartgrade.common.storage;

/** Object keys are opaque references; original files never enter business tables. */
public interface ObjectStorage {
    String store(String key, byte[] bytes, String contentType);
    byte[] read(String key);
}
