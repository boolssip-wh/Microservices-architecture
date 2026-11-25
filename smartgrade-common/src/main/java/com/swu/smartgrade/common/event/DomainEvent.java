package com.swu.smartgrade.common.event;

public record DomainEvent(String eventId, String type, Long teacherId,
                          String batchId, String title, String content) {
}
