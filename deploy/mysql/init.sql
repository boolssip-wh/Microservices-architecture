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
