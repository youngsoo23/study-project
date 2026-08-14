package com.young.studyproject.document.infrastructure;

import com.young.studyproject.document.application.DocumentService;
import com.young.studyproject.outbox.domain.PaymentEventTopics;
import com.young.studyproject.payment.application.dto.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * "payment-completed" 토픽을 구독해 문서를 생성하는 Consumer.
 * 예외를 던지면 KafkaConfig의 DefaultErrorHandler가 재시도하고, 모두 실패하면 DLT로 보낸다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentConsumer {

    private final DocumentService documentService;

    @KafkaListener(topics = PaymentEventTopics.PAYMENT_COMPLETED, groupId = "document-service")
    public void listen(PaymentCompletedEvent event) {
        log.info("[DOCUMENT] 이벤트 수신 paymentId={} eventId={}", event.paymentId(), event.eventId());

        if (event.forceFailure()) {
            throw new RuntimeException("문서 생성 강제 실패 paymentId=" + event.paymentId());
        }

        documentService.createIfAbsent(event.paymentId(), event.orderId());
    }
}
