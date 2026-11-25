package com.swu.smartgrade.grading.infrastructure.events;

import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** C04 owns event consumption; C03 keeps its import-job and answer state in its own database. */
@Component
@Profile("infra")
public class KafkaImportConsumer {
    private final ImportJobClient jobs;
    public KafkaImportConsumer(ImportJobClient jobs) { this.jobs = jobs; }

    @KafkaListener(topics = "grading.requested", groupId = "smartgrade-grading-dispatch")
    public void consume(String jobId) {
        if (jobId == null || jobId.isBlank()) throw new IllegalArgumentException("Missing import job ID");
        var response = jobs.process(jobId);
        if (response == null || response.data() == null) {
            throw new IllegalStateException("Import-job processing did not acknowledge completion");
        }
    }
}
