CREATE TABLE sms_delivery (
    delivery_id VARCHAR(64) PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    event_id VARCHAR(64),
    event_type VARCHAR(32) NOT NULL,
    target_masked VARCHAR(32) NOT NULL,
    message TEXT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_sms_delivery_teacher ON sms_delivery(teacher_id, created_at);
CREATE UNIQUE INDEX uk_sms_delivery_event_teacher ON sms_delivery(teacher_id, event_id);
