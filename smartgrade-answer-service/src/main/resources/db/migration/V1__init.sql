CREATE TABLE batch (
    batch_id VARCHAR(64) PRIMARY KEY,
    teacher_id BIGINT NOT NULL,
    exam_name VARCHAR(255) NOT NULL,
    question_no VARCHAR(64) NOT NULL,
    question_title VARCHAR(255) NOT NULL,
    max_score DECIMAL(10,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE import_job (
    job_id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    numbering_rule VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    accepted_count INT NOT NULL,
    rejected_count INT NOT NULL,
    validation_json TEXT,
    created_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP
);

CREATE TABLE answer (
    answer_id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL,
    import_job_id VARCHAR(64) NOT NULL,
    student_no VARCHAR(64) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    ocr_text TEXT,
    ocr_confidence DECIMAL(5,2),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_answer_job_student UNIQUE (import_job_id, student_no)
);

CREATE INDEX idx_batch_teacher ON batch(teacher_id);
CREATE INDEX idx_answer_batch ON answer(batch_id);
