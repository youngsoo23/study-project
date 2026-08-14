package com.young.studyproject.outbox.application;

/**
 * Payment 등 다른 모듈이 자신의 DB 트랜잭션 안에서 Kafka로 보낼 이벤트를 Outbox에 저장할 때 사용하는 포트.
 * Kafka 발행 자체는 여기서 하지 않는다 — Kafka는 DB 트랜잭션에 참여하지 않으므로, 실제 발행은
 * OutboxPublisher가 트랜잭션이 끝난 뒤 별도 스케줄로 처리한다.
 */
public interface OutboxEventRecorder {

    void record(String eventId, String eventType, Long aggregateId, Object payload);
}
