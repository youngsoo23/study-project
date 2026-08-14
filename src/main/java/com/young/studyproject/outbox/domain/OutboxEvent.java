package com.young.studyproject.outbox.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OutboxEvent {

    private final Long id;
    private final String eventId;
    private final String eventType;
    private final Long aggregateId;
    private final String payload;
    private final OutboxEventStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime publishedAt;

    public OutboxEvent publish() {
        return OutboxEvent.builder()
                .id(this.id)
                .eventId(this.eventId)
                .eventType(this.eventType)
                .aggregateId(this.aggregateId)
                .payload(this.payload)
                .status(OutboxEventStatus.PUBLISHED)
                .createdAt(this.createdAt)
                .publishedAt(LocalDateTime.now())
                .build();
    }
}
