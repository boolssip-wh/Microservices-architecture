CREATE TABLE notification (
    notification_id VARCHAR(64) PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    batch_id VARCHAR(64),
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_notification_teacher ON notification(teacher_id);
