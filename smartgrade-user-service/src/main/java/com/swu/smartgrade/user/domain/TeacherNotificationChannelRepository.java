package com.swu.smartgrade.user.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherNotificationChannelRepository
        extends JpaRepository<TeacherNotificationChannel, Long> {
}
