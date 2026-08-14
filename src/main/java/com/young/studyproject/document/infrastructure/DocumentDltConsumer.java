package com.young.studyproject.document.infrastructure;

import com.young.studyproject.outbox.domain.PaymentEventTopics;
import com.young.studyproject.payment.application.dto.PaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Retry를 모두 실패해 DLT("payment-completed.DLT")로 넘어온 메시지를 확인하는 용도의 Consumer.
 * 원본 토픽으로 무한 재발행하지 않고 로그만 남긴다 — 실제 운영에서는 알림/수동 재처리로 이어지는 지점이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentDltConsumer {

    private final DltEventMonitor dltEventMonitor;

    @KafkaListener(topics = PaymentEventTopics.PAYMENT_COMPLETED_DLT, groupId = "document-dlt-monitor")
    public void listen(PaymentCompletedEvent event) {
        log.warn("[DLT] 문서 생성 최종 실패 eventId={} paymentId={} orderId={}",
                event.eventId(), event.paymentId(), event.orderId());
        dltEventMonitor.record(event);
    }
}
