-- C04 owns grading_result/hit_evidence. C05 stores a local read projection for review.
ALTER TABLE grading_result RENAME TO review_result_projection;
ALTER TABLE hit_evidence RENAME TO review_evidence_projection;
ALTER TABLE review_result_projection ADD COLUMN scoring_points_json TEXT;
-- A pending review/draft has no final score or confirmation time yet.
ALTER TABLE review_record MODIFY COLUMN final_score DECIMAL(10,2) NULL;
ALTER TABLE review_record MODIFY COLUMN confirmed_at TIMESTAMP NULL;
