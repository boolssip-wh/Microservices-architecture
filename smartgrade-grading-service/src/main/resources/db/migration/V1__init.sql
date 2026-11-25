CREATE TABLE grading_task (
    task_id VARCHAR(64) PRIMARY KEY,
    answer_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL,
    error_code VARCHAR(64),
    normalized_text TEXT,
    confidence DECIMAL(5,2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
