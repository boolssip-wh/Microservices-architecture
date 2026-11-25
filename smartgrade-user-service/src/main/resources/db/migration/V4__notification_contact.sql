ALTER TABLE teacher_notification_channel ADD COLUMN contact VARCHAR(128);
UPDATE teacher_notification_channel SET contact = phone WHERE contact IS NULL;
