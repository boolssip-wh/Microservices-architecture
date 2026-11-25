package com.swu.smartgrade.answer.application;

import com.swu.smartgrade.answer.domain.ImportJobRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@EnableScheduling
public class ImportEventDispatcher {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ImportEventDispatcher.class);
    private final GradingOrchestrator orchestrator;
    private final ObjectProvider<KafkaTemplate<String, String>> kafka;
    private final boolean infra;
    private final ImportJobRepository jobs;
    public ImportEventDispatcher(GradingOrchestrator orchestrator, ObjectProvider<KafkaTemplate<String, String>> kafka,
                                 Environment environment, ImportJobRepository jobs) {
        this.orchestrator = orchestrator; this.kafka = kafka; this.jobs = jobs;
        infra = environment.acceptsProfiles(Profiles.of("infra"));
    }
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void dispatch(ImportReadyEvent event) { deliver(event); }

    // PENDING jobs serve as a durable hand-off record if publishing after commit fails.
    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void recoverPending() {
        for (var job : jobs.findByStatus("PENDING")) deliver(new ImportReadyEvent(job.getJobId()));
    }
    private void deliver(ImportReadyEvent event) {
        try {
            if (infra) kafka.getObject().send("grading.requested", event.jobId(), event.jobId()).get(10, java.util.concurrent.TimeUnit.SECONDS);
            else orchestrator.process(event);
        } catch (Exception ex) { LOG.warn("导入任务 {} 等待再次派发", event.jobId(), ex); }
    }
}
