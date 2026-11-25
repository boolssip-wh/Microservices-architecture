CREATE TABLE teacher_notification_channel (
    teacher_id BIGINT PRIMARY KEY,
    phone VARCHAR(24) NOT NULL,
    sms_enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
