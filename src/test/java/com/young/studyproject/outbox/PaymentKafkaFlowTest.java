package com.young.studyproject.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.young.studyproject.document.domain.DocumentRepository;
import com.young.studyproject.document.infrastructure.DltEventMonitor;
import com.young.studyproject.outbox.application.OutboxPublisher;
import com.young.studyproject.outbox.domain.PaymentEventTopics;
import com.young.studyproject.payment.application.PaymentService;
import com.young.studyproject.payment.application.dto.PaymentCreateCommand;
import com.young.studyproject.payment.application.dto.PaymentResult;
import java.time.Duration;
import java.time.Instant;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

/**
 * Payment complete -> Outbox -> Kafka -> Document Consumer -> (실패 시) Retry -> DLT 전체 흐름을
 * 실제(임베디드) Kafka 브로커로 검증한다. 브로커 기동과 FixedBackOff 재시도 대기 때문에 다른 테스트보다 느리다.
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {PaymentEventTopics.PAYMENT_COMPLETED, PaymentEventTopics.PAYMENT_COMPLETED_DLT})
@TestPropertySource(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
class PaymentKafkaFlowTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DltEventMonitor dltEventMonitor;

    @Test
    @DisplayName("정상 흐름: Outbox 발행 -> Kafka -> Document Consumer -> 문서 생성")
    void publishedEventCreatesDocument() {
        PaymentResult payment = paymentService.create(new PaymentCreateCommand("ORDER-KAFKA-OK"));
        paymentService.complete(payment.id());

        outboxPublisher.publishPending();

        waitUntil(() -> documentRepository.existsByPaymentId(payment.id()), Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("실패 흐름: 강제 실패 -> Retry 모두 실패 -> DLT 도달")
    void forcedFailureEndsUpInDlt() {
        PaymentResult payment = paymentService.create(new PaymentCreateCommand("ORDER-KAFKA-FAIL"));
        paymentService.markDocumentFailure(payment.id());
        paymentService.complete(payment.id());

        outboxPublisher.publishPending();

        waitUntil(() -> dltEventMonitor.recentEvents().stream()
                .anyMatch(event -> event.paymentId().equals(payment.id())), Duration.ofSeconds(15));

        assertThat(documentRepository.existsByPaymentId(payment.id())).isFalse();
    }

    private void waitUntil(BooleanSupplier condition, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            sleep(200);
        }
        Assertions.fail("조건이 제한 시간(" + timeout + ") 내에 충족되지 않았습니다.");
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
