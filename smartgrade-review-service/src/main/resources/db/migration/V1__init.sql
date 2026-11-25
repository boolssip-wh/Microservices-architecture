CREATE TABLE grading_result (
    result_id VARCHAR(64) PRIMARY KEY,
    answer_id VARCHAR(64) NOT NULL UNIQUE,
    batch_id VARCHAR(64) NOT NULL,
    student_no VARCHAR(64) NOT NULL,
    answer_text TEXT,
    teacher_id BIGINT NOT NULL,
    rubric_version INT NOT NULL,
    suggested_score DECIMAL(10,2) NOT NULL,
    max_score DECIMAL(10,2) NOT NULL,
    confidence DECIMAL(5,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    final_score DECIMAL(10,2),
    created_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP
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

CREATE TABLE review_record (
    review_id VARCHAR(64) PRIMARY KEY,
    result_id VARCHAR(64) NOT NULL UNIQUE,
    teacher_id BIGINT NOT NULL,
    final_score DECIMAL(10,2) NOT NULL,
    review_status VARCHAR(32) NOT NULL,
    review_note TEXT,
    confirmed_at TIMESTAMP NOT NULL
);

CREATE TABLE score_snapshot (
    snapshot_id VARCHAR(64) PRIMARY KEY,
    review_id VARCHAR(64) NOT NULL,
    final_score DECIMAL(10,2) NOT NULL,
    snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE comment_draft (
    comment_id VARCHAR(64) PRIMARY KEY,
    review_id VARCHAR(64) NOT NULL,
    teacher_id BIGINT NOT NULL,
    profile_id BIGINT,
    profile_version INT,
    suggested_text TEXT,
    final_text TEXT,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE audit_log (
    audit_id VARCHAR(64) PRIMARY KEY,
    actor_id BIGINT NOT NULL,
    action VARCHAR(64) NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    object_id VARCHAR(64) NOT NULL,
    summary_json TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_result_batch ON grading_result(batch_id);
CREATE INDEX idx_result_teacher ON grading_result(teacher_id);
CREATE INDEX idx_evidence_result ON hit_evidence(result_id);
