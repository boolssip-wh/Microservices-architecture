ALTER TABLE grading_task ADD COLUMN rubric_version INT;
ALTER TABLE grading_task ADD COLUMN idempotency_key VARCHAR(255);
ALTER TABLE grading_task ADD COLUMN retry_keys TEXT;

CREATE TABLE grading_result (
    result_id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    answer_id VARCHAR(64),
    rubric_version INT NOT NULL,
    suggested_score DECIMAL(10,2) NOT NULL,
    confidence DECIMAL(5,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE TABLE hit_evidence (
    evidence_id VARCHAR(64) PRIMARY KEY,
    result_id VARCHAR(64) NOT NULL,
    point_id VARCHAR(64) NOT NULL,
    point_name VARCHAR(255) NOT NULL,
    match_type VARCHAR(32) NOT NULL,
    sentence TEXT NOT NULL,
    start_offset INT NOT NULL,
    end_offset INT NOT NULL,
    confidence DECIMAL(5,2) NOT NULL
);
CREATE INDEX idx_grading_evidence_result ON hit_evidence(result_id);
