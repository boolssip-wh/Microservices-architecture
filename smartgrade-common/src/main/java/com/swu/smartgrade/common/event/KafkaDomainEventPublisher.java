package com.swu.smartgrade.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
@Profile("infra")
public class KafkaDomainEventPublisher extends AfterCommitEventPublisher {
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    public KafkaDomainEventPublisher(KafkaTemplate<String, String> kafka, ObjectMapper json) {
        this.kafka = kafka; this.json = json;
    }
    protected void send(DomainEvent event) throws Exception {
        if (!java.util.Set.of("grading.started", "grading.completed", "grading.failed", "report.completed",
                "report.failed", "review.confirmed", "review.completed").contains(event.type())) {
            throw new IllegalArgumentException("Unsupported domain event type");
        }
        kafka.send(event.type(), event.eventId(), json.writeValueAsString(event)).get(5, TimeUnit.SECONDS);
    }
}
