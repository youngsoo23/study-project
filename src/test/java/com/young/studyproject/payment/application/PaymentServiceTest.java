package com.young.studyproject.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.young.studyproject.outbox.domain.OutboxEvent;
import com.young.studyproject.outbox.domain.OutboxEventStatus;
import com.young.studyproject.outbox.domain.OutboxRepository;
import com.young.studyproject.payment.application.dto.PaymentCreateCommand;
import com.young.studyproject.payment.application.dto.PaymentResult;
import com.young.studyproject.payment.domain.PaymentStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @DisplayName("결제를 완료하면 같은 트랜잭션에서 Outbox에 PENDING 이벤트가 저장된다")
    void completeRecordsOutboxEvent() {
        PaymentResult created = paymentService.create(new PaymentCreateCommand("ORDER-1"));

        PaymentResult completed = paymentService.complete(created.id());

        assertThat(completed.status()).isEqualTo(PaymentStatus.COMPLETE);

        List<OutboxEvent> pending = outboxRepository.findPending(50);
        assertThat(pending)
                .anySatisfy(event -> {
                    assertThat(event.getAggregateId()).isEqualTo(completed.id());
                    assertThat(event.getEventType()).isEqualTo("PAYMENT_COMPLETED");
                    assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
                    assertThat(event.getPayload()).contains("\"paymentId\":" + completed.id());
                });
    }

    @Test
    @DisplayName("이미 완료된 결제를 다시 완료하면 IllegalArgumentException")
    void completeTwiceRejected() {
        PaymentResult created = paymentService.create(new PaymentCreateCommand("ORDER-2"));
        paymentService.complete(created.id());

        assertThatThrownBy(() -> paymentService.complete(created.id()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("완료된 결제에는 문서 생성 강제 실패 플래그를 설정할 수 없다")
    void markDocumentFailureRejectedAfterComplete() {
        PaymentResult created = paymentService.create(new PaymentCreateCommand("ORDER-3"));
        paymentService.complete(created.id());

        assertThatThrownBy(() -> paymentService.markDocumentFailure(created.id()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
