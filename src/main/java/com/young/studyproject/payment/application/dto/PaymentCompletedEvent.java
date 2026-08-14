package com.young.studyproject.payment.application.dto;

/**
 * 결제 완료 시 Outbox에 저장되고 Kafka "payment-completed" 토픽으로 발행되는 이벤트.
 *
 * <p>forceFailure는 Document Consumer의 Retry/DLT 동작을 학습용으로 강제 실행해보기 위한 플래그다.
 * 실제 서비스라면 존재하지 않을 필드지만, 여기서는 이 값을 이벤트에 실어 보내는 것만으로
 * Consumer가 무엇을 실패시켜야 하는지 알 수 있게 한다(Payment DB를 다시 조회할 필요가 없다).
 */
public record PaymentCompletedEvent(String eventId, Long paymentId, String orderId, boolean forceFailure) {
}
