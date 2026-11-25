ALTER TABLE review_result_projection ADD COLUMN base_score DECIMAL(10,2);
UPDATE review_result_projection SET base_score = CASE WHEN max_score < 10 THEN max_score ELSE 10 END;
ALTER TABLE review_result_projection MODIFY COLUMN base_score DECIMAL(10,2) NOT NULL;
