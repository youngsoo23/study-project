package com.young.studyproject.payment.application;

import com.young.studyproject.common.exception.EntityNotFoundException;
import com.young.studyproject.outbox.application.OutboxEventRecorder;
import com.young.studyproject.payment.application.dto.PaymentCompletedEvent;
import com.young.studyproject.payment.application.dto.PaymentCreateCommand;
import com.young.studyproject.payment.application.dto.PaymentResult;
import com.young.studyproject.payment.domain.Payment;
import com.young.studyproject.payment.domain.PaymentRepository;
import com.young.studyproject.payment.domain.PaymentStatus;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private static final String EVENT_TYPE_PAYMENT_COMPLETED = "PAYMENT_COMPLETED";

    private final PaymentRepository paymentRepository;
    private final OutboxEventRecorder outboxEventRecorder;

    @Transactional
    public PaymentResult create(PaymentCreateCommand command) {
        Payment payment = Payment.builder()
                .orderId(command.orderId())
                .status(PaymentStatus.READY)
                .documentFailureForced(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return PaymentResult.from(paymentRepository.save(payment));
    }

    public PaymentResult getById(Long id) {
        return PaymentResult.from(findPayment(id));
    }

    @Transactional
    public PaymentResult markDocumentFailure(Long id) {
        Payment payment = findPayment(id);
        if (payment.getStatus() == PaymentStatus.COMPLETE) {
            throw new IllegalArgumentException("이미 완료된 결제에는 강제 실패를 설정할 수 없습니다. id=" + id);
        }

        return PaymentResult.from(paymentRepository.save(payment.forceDocumentFailure()));
    }

    /**
     * Payment DB 변경과 Kafka 발행은 하나의 트랜잭션으로 묶기 어렵기 때문에(Kafka는 DB 트랜잭션에 참여하지 않는다),
     * Kafka에 보낼 이벤트를 같은 DB 트랜잭션 안에서 Outbox 테이블에 먼저 저장한다. 실제 Kafka 발행은
     * OutboxPublisher가 별도 스케줄로 뒤늦게 처리하므로, 이 메서드가 끝난 시점엔 아직 Kafka에 발행되지 않은 상태다.
     */
    @Transactional
    public PaymentResult complete(Long id) {
        Payment payment = findPayment(id);
        if (payment.getStatus() == PaymentStatus.COMPLETE) {
            throw new IllegalArgumentException("이미 완료된 결제입니다. id=" + id);
        }

        Payment completed = paymentRepository.save(payment.complete());
        log.info("[PAYMENT] 결제 완료 paymentId={}", completed.getId());

        String eventId = UUID.randomUUID().toString();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                eventId, completed.getId(), completed.getOrderId(), completed.isDocumentFailureForced());
        outboxEventRecorder.record(eventId, EVENT_TYPE_PAYMENT_COMPLETED, completed.getId(), event);

        return PaymentResult.from(completed);
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("결제를 찾을 수 없습니다. id=" + id));
    }
}
