ALTER TABLE grading_result ADD COLUMN base_score DECIMAL(10,2);
-- Historical results do not include max_score, so their original base is unknown.
-- New matches persist the base selected by the owning batch.
