package com.swu.smartgrade.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    private String notificationId;
    private Long teacherId;
    private String type;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String batchId;
    @Column(unique = true)
    private String eventId;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
