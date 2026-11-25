CREATE TABLE rubric (
    rubric_id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL,
    source_text TEXT NOT NULL,
    max_score DECIMAL(10,2) NOT NULL,
    version INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    checksum VARCHAR(128),
    created_at TIMESTAMP NOT NULL,
    compiled_at TIMESTAMP
);

CREATE TABLE rubric_point (
    point_id VARCHAR(64) PRIMARY KEY,
    rubric_id VARCHAR(64) NOT NULL,
    point_code VARCHAR(32) NOT NULL,
    point_name VARCHAR(255) NOT NULL,
    point_type VARCHAR(32) NOT NULL,
    weight DECIMAL(10,2) NOT NULL,
    max_score DECIMAL(10,2) NOT NULL,
    keywords TEXT
);

CREATE TABLE point_variant (
    variant_id VARCHAR(64) PRIMARY KEY,
    point_id VARCHAR(64) NOT NULL,
    variant_text VARCHAR(255) NOT NULL,
    match_type VARCHAR(32) NOT NULL,
    confidence DECIMAL(5,2) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE rubric_version_snapshot (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    rubric_id VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    payload TEXT NOT NULL,
    checksum VARCHAR(128) NOT NULL,
    compiled_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_rubric_version UNIQUE (rubric_id, version)
);
