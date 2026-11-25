package com.swu.smartgrade.notification.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swu.smartgrade.common.event.DomainEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("infra")
public class NotificationEventListener {
    private final NotificationApplicationService service;
    private final ObjectMapper json;
    public NotificationEventListener(NotificationApplicationService service, ObjectMapper json) {
        this.service = service; this.json = json;
    }
    @KafkaListener(topics = {"grading.completed", "grading.failed", "report.completed", "report.failed", "review.confirmed"},
            groupId = "smartgrade-notification-service")
    public void receive(String payload) throws Exception {
        service.consume(json.readValue(payload, DomainEvent.class));
    }
}
