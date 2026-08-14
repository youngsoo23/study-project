package com.young.studyproject.outbox.application;

import com.young.studyproject.outbox.domain.OutboxEvent;
import com.young.studyproject.outbox.domain.OutboxEventStatus;
import com.young.studyproject.outbox.domain.OutboxRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventRecorderImpl implements OutboxEventRecorder {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void record(String eventId, String eventType, Long aggregateId, Object payload) {
        OutboxEvent event = OutboxEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .payload(objectMapper.writeValueAsString(payload))
                .status(OutboxEventStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(event);
        log.info("[OUTBOX] 이벤트 저장 eventId={} eventType={} aggregateId={}", eventId, eventType, aggregateId);
    }
}
