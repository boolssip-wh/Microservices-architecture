CREATE TABLE export_record (
    export_id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL,
    teacher_id BIGINT NOT NULL,
    format VARCHAR(16) NOT NULL,
    file_key VARCHAR(512),
    status VARCHAR(32) NOT NULL,
    audit_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_export_teacher ON export_record(teacher_id);
