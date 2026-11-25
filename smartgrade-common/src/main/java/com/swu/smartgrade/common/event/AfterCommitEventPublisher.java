package com.swu.smartgrade.common.event;

import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Notifications run only after a successful transaction and cannot roll back grading. */
public abstract class AfterCommitEventPublisher implements DomainEventPublisher {
    public final void publish(DomainEvent event) {
        Runnable send = () -> {
            try { send(event); }
            catch (Exception ex) {
                LoggerFactory.getLogger(getClass()).error("Event delivery failed: {} {}", event.eventId(), event.type(), ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { send.run(); }
            });
        } else { send.run(); }
    }
    protected abstract void send(DomainEvent event) throws Exception;
}
