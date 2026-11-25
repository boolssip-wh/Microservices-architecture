CREATE TABLE teacher (
    teacher_id BIGINT PRIMARY KEY,
    account VARCHAR(64) NOT NULL UNIQUE,
    display_name VARCHAR(64) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    password_salt VARCHAR(128) NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE teacher_comment_profile (
    profile_id BIGINT PRIMARY KEY,
    teacher_id BIGINT NOT NULL UNIQUE,
    tone VARCHAR(64) NOT NULL,
    length_mode VARCHAR(64) NOT NULL,
    opening_style VARCHAR(64) NOT NULL,
    common_phrases TEXT,
    banned_phrases TEXT,
    profile_version INT NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
