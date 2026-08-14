package com.young.studyproject.outbox.domain;

/**
 * OutboxPublisher(발행)와 Document 모듈의 Kafka Listener(구독)가 동일한 문자열을 참조하도록 상수로 공유한다.
 * {@code @KafkaListener(topics = ...)}는 컴파일 타임 상수가 필요해 여기서도 상수로 둔다.
 */
public final class PaymentEventTopics {

    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String PAYMENT_COMPLETED_DLT = "payment-completed.DLT";

    private PaymentEventTopics() {
    }
}
