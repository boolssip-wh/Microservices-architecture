-- A5 数据隔离初始化：每个业务服务使用自己的数据库，服务之间只通过业务 ID 协作。
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
-- 演示账号只获得本项目 7 个业务库的权限，便于现场说明数据归属。
GRANT ALL PRIVILEGES ON smartgrade_user.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_rubric.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_answer.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_grading.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_review.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_report.* TO 'smartgrade'@'%';
GRANT ALL PRIVILEGES ON smartgrade_notification.* TO 'smartgrade'@'%';
FLUSH PRIVILEGES;

-- D1 Seata AT 回滚证据：answer-service 与 review-service 各自维护 undo_log。
-- Seata 会在 Flyway 之前代理数据源，因此 undo_log 必须在初始化阶段先存在。
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
