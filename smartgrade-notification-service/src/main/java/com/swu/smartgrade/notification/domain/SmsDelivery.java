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
@Table(name = "sms_delivery")
public class SmsDelivery {
    @Id
    private String deliveryId;
    private Long teacherId;
    private String eventId;
    private String eventType;
    private String targetMasked;
    @Column(columnDefinition = "TEXT")
    private String message;
    private String provider;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
