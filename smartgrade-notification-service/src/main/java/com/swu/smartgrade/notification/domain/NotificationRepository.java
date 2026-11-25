package com.swu.smartgrade.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    java.util.Optional<Notification> findByEventId(String eventId);
    List<Notification> findByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    List<Notification> findByTeacherIdAndReadAtIsNullOrderByCreatedAtDesc(Long teacherId);
}
