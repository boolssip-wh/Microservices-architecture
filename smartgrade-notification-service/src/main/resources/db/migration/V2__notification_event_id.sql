ALTER TABLE notification ADD COLUMN event_id VARCHAR(64);
CREATE UNIQUE INDEX uk_notification_event ON notification(event_id);
