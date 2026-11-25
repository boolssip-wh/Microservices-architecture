package com.swu.smartgrade.notification.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SmsDeliveryRepository extends JpaRepository<SmsDelivery, String> {
    List<SmsDelivery> findTop20ByTeacherIdOrderByCreatedAtDesc(Long teacherId);

    boolean existsByTeacherIdAndEventId(Long teacherId, String eventId);
}
