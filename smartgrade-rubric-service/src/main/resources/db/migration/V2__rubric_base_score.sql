ALTER TABLE rubric ADD COLUMN base_score DECIMAL(10,2);
UPDATE rubric SET base_score = CASE WHEN max_score < 10 THEN max_score ELSE 10 END;
ALTER TABLE rubric MODIFY COLUMN base_score DECIMAL(10,2) NOT NULL;
