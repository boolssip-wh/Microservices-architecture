CREATE DATABASE IF NOT EXISTS smartgrade_user
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS smartgrade_rubric
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS smartgrade_answer
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS smartgrade_grading
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS smartgrade_review
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS smartgrade_report
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS smartgrade_notification
    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'smartgrade'@'%' IDENTIFIED BY 'smartgrade123';
GRANT ALL PRIVILEGES ON smartgrade_user.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_rubric.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_answer.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_grading.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_review.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_report.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_notification.* TO 'smartgrade'@'%';
FLUSH PRIVILEGES;

-- Seata wraps the data source before Flyway starts, so AT undo logs must exist
-- during database initialization rather than only in application migrations.
USE smartgrade_answer;
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id BIGINT NOT NULL,
    xid VARCHAR(128) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB NOT NULL,
    log_status INT NOT NULL,
    log_created TIMESTAMP(6) NOT NULL,
    log_modified TIMESTAMP(6) NOT NULL,
    ext VARCHAR(100),
    CONSTRAINT ux_answer_undo_log UNIQUE (xid, branch_id)
);
CREATE INDEX ix_answer_undo_log_status ON undo_log(log_status);

USE smartgrade_review;
CREATE TABLE IF NOT EXISTS undo_log (
    branch_id BIGINT NOT NULL,
    xid VARCHAR(128) NOT NULL,
    context VARCHAR(128) NOT NULL,
    rollback_info LONGBLOB NOT NULL,
    log_status INT NOT NULL,
    log_created TIMESTAMP(6) NOT NULL,
    log_modified TIMESTAMP(6) NOT NULL,
    ext VARCHAR(100),
    CONSTRAINT ux_review_undo_log UNIQUE (xid, branch_id)
);
CREATE INDEX ix_review_undo_log_status ON undo_log(log_status);
