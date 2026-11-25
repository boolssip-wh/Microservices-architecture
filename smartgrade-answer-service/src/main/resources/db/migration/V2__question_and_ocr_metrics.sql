ALTER TABLE batch ADD COLUMN question_text TEXT;
ALTER TABLE answer ADD COLUMN ocr_coverage_ratio DECIMAL(5,2);
ALTER TABLE answer ADD COLUMN ocr_character_count INT;
