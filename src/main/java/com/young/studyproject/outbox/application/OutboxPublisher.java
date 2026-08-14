package com.young.studyproject.outbox.application;

import com.young.studyproject.outbox.domain.OutboxEvent;
import com.young.studyproject.outbox.domain.OutboxRepository;
import com.young.studyproject.outbox.domain.PaymentEventTopics;
import com.young.studyproject.payment.application.dto.PaymentCompletedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * DB에 안전하게 저장된 PENDING Outbox 이벤트를 Kafka로 전달한다.
 *
 * <p>학습하기 쉽게 폴링 방식(@Scheduled)으로 구현했다. Kafka 발행이 실패해도 Outbox 상태를
 * PENDING으로 그대로 두면 다음 스케줄에서 다시 시도되므로 이벤트가 유실되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int BATCH_SIZE = 50;

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;
    // Spring Boot가 자동 구성하는 빈 타입(KafkaTemplate<Object, Object>)과 정확히 맞춰야 주입된다.
    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPending() {
        List<OutboxEvent> pendingEvents = outboxRepository.findPending(BATCH_SIZE);
        pendingEvents.forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        try {
            PaymentCompletedEvent payload = objectMapper.readValue(event.getPayload(), PaymentCompletedEvent.class);
            log.info("[OUTBOX-PUBLISHER] Kafka 발행 시작 eventId={}", event.getEventId());

            kafkaTemplate.send(PaymentEventTopics.PAYMENT_COMPLETED, payload.paymentId().toString(), payload).get();

            outboxRepository.save(event.publish());
            log.info("[KAFKA] PAYMENT_COMPLETED 발행 성공 eventId={}", event.getEventId());
        } catch (Exception e) {
            log.warn("[OUTBOX-PUBLISHER] Kafka 발행 실패, 다음 스케줄에서 재시도합니다. eventId={}", event.getEventId(), e);
        }
    }
}
