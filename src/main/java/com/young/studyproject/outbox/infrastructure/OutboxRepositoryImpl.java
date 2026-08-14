package com.young.studyproject.outbox.infrastructure;

import com.young.studyproject.outbox.domain.OutboxEvent;
import com.young.studyproject.outbox.domain.OutboxEventStatus;
import com.young.studyproject.outbox.domain.OutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OutboxRepositoryImpl implements OutboxRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return toDomain(outboxEventJpaRepository.save(toEntity(event)));
    }

    @Override
    public List<OutboxEvent> findPending(int limit) {
        return outboxEventJpaRepository
                .findByStatusOrderByIdAsc(OutboxEventStatus.PENDING, PageRequest.of(0, limit))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private OutboxEventJpaEntity toEntity(OutboxEvent event) {
        return OutboxEventJpaEntity.builder()
                .id(event.getId())
                .eventId(event.getEventId())
                .eventType(event.getEventType())
                .aggregateId(event.getAggregateId())
                .payload(event.getPayload())
                .status(event.getStatus())
                .createdAt(event.getCreatedAt())
                .publishedAt(event.getPublishedAt())
                .build();
    }

    private OutboxEvent toDomain(OutboxEventJpaEntity entity) {
        return OutboxEvent.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .aggregateId(entity.getAggregateId())
                .payload(entity.getPayload())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .publishedAt(entity.getPublishedAt())
                .build();
    }
}
