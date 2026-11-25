ALTER TABLE batch ADD COLUMN base_score DECIMAL(10,2);
UPDATE batch SET base_score = CASE WHEN max_score < 10 THEN max_score ELSE 10 END;
ALTER TABLE batch MODIFY COLUMN base_score DECIMAL(10,2) NOT NULL;
