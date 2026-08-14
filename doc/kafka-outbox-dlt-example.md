# Kafka Outbox / Retry / DLT 학습 예제

결제 완료라는 하나의 이벤트가 DB 트랜잭션 → Outbox → Kafka → Consumer → (실패 시) Retry → DLT로
이어지는 흐름을, DB/Kafka 상태와 콘솔 로그로 직접 눈으로 확인하는 예제.
원래 요구사항은 `doc/claude-code-kafka-outbox-dlt-study-prompt.md`에 있다.

## 전체 흐름

```
POST /api/payments                        → Payment(READY) 생성
POST /api/payments/{id}/document-failure   → 강제 실패 플래그 설정 (선택, complete 이전에만)
POST /api/payments/{id}/complete           → Payment(COMPLETE) + OutboxEvent(PENDING) 같은 트랜잭션 저장
  ↓ OutboxPublisher (@Scheduled, 1초 간격)
OutboxRepository.findPending(50) → Kafka "payment-completed" 발행 → OutboxEvent(PUBLISHED)
  ↓
DocumentConsumer (@KafkaListener, groupId=document-service)
  → forceFailure=true 면 예외, 아니면 Document existsByPaymentId 확인 후 생성(멱등)
  ↓ 실패 시
DefaultErrorHandler(FixedBackOff 1초 × 2회) 재시도 → 모두 실패
  ↓
DeadLetterPublishingRecoverer → "payment-completed.DLT" 로 자동 재발행
  ↓
DocumentDltConsumer (@KafkaListener, groupId=document-dlt-monitor) → 로그 + DltEventMonitor 기록
```

## 구성

- `com.young.studyproject.payment` — `Payment`(READY/COMPLETE), `PaymentService`, `PaymentController`.
  `PaymentCompletedEvent`(Kafka 메시지 계약)도 `payment.application.dto`에 있다.
- `com.young.studyproject.outbox` — `OutboxEvent`(PENDING/PUBLISHED), `OutboxEventRecorder`(Payment가
  트랜잭션 안에서 이벤트를 저장할 때 쓰는 포트), `OutboxPublisher`(`@Scheduled`로 Kafka 발행).
- `com.young.studyproject.document` — `Document`, `DocumentService`(멱등 생성), `DocumentConsumer`,
  `DocumentDltConsumer`, `DltEventMonitor`(최근 DLT 수신 확인용).
- `com.young.studyproject.common.config.KafkaConfig` — 재시도/DLT 공통 정책(`DefaultErrorHandler` +
  `DeadLetterPublishingRecoverer` + `FixedBackOff`).
- `com.young.studyproject.common.config.SchedulingConfig` — `@EnableScheduling`.
- 루트 `docker-compose.yml` — 로컬 Kafka(단일 노드 KRaft) + 선택적 kafka-ui.

## 정리해 둘 내용

### 1. Outbox가 필요한 이유

`Payment DB 저장 성공, Kafka 발행 실패`라는 상황이 문제가 되는 이유는, Payment는 이미
COMPLETE로 커밋됐는데 Kafka 메시지가 사라지면 Document Consumer는 그 사실을 영원히 알 수 없기
때문이다. DB 트랜잭션과 Kafka 발행은 애초에 하나의 트랜잭션으로 묶을 수 없다(서로 다른 시스템).

Outbox 패턴은 "Kafka에 보낼 이벤트"를 Payment와 **같은 DB, 같은 트랜잭션**에 먼저 저장해버린다
(`PaymentService.complete()`). Payment 저장이 성공하면 Outbox 행도 반드시 함께 커밋되고, 실패하면
둘 다 롤백된다. 실제 Kafka 발행은 `OutboxPublisher`가 트랜잭션이 끝난 뒤 별도로, 성공할 때까지
재시도하며 처리한다 — Kafka가 잠깐 죽어 있어도 이벤트는 DB에 PENDING으로 안전하게 남는다.

### 2. DLT가 필요한 이유

`Kafka 메시지 수신 성공, Document 생성 계속 실패`라는 상황에서, 재시도만 하고 끝내면 그 메시지는
컨슈머 오프셋이 넘어가지 않아 뒤에 있는 정상 메시지들까지 전부 막힌다(head-of-line blocking).
그렇다고 그냥 버리면 실패한 이벤트가 조용히 유실된다.

DLT(Dead Letter Topic)는 정해진 횟수(여기서는 1초 간격 총 3회)만큼 재시도해도 계속 실패하는
메시지만 별도 토픽(`payment-completed.DLT`)으로 옮겨서, 원본 토픽의 나머지 메시지는 계속 처리되게
하고, 실패한 메시지는 버리지 않고 나중에 확인/재처리할 수 있게 보관한다.

### 3. 멱등성

Kafka는 최소 한 번 전달(at-least-once)을 기본으로 하므로 같은 메시지가 중복 전달될 수 있다.
`DocumentService.createIfAbsent()`는 저장 전에 `existsByPaymentId`로 먼저 확인하고, DB의
`documents.payment_id` UNIQUE 제약으로 한 번 더 방어한다.

### 4. 강제 실패는 이벤트에 실어 보낸다

Retry/DLT를 직접 보려면 실패를 재현할 방법이 필요하다. `POST /payments/{id}/document-failure`로
Payment에 `documentFailureForced` 플래그를 켜두면, `complete()` 시 이 값이 `PaymentCompletedEvent.forceFailure`에
그대로 담겨 Kafka로 전달된다. `DocumentConsumer`는 이 값만 보고 예외를 던지므로 Payment DB를
다시 조회할 필요가 없다.

## 실행 및 테스트

```bash
docker compose up -d
./gradlew bootRun
```

- H2 콘솔(`http://localhost:8080/h2-console`, JDBC URL `jdbc:h2:mem:studydb;MODE=MySQL`)에서
  `outbox_events`, `documents` 테이블 상태를 직접 확인할 수 있다.
- kafka-ui(`http://localhost:8090`, docker-compose에 포함)에서 `payment-completed`,
  `payment-completed.DLT` 토픽의 실제 메시지를 볼 수 있다.

### 시나리오 A — 정상 처리

```bash
curl -X POST http://localhost:8080/api/payments -H "Content-Type: application/json" \
  -d '{"orderId":"ORDER-SUCCESS-001"}'
# 응답의 id를 아래 {id}에 사용
curl -X POST http://localhost:8080/api/payments/{id}/complete
curl http://localhost:8080/api/payments/{id}
```

콘솔 로그가 `[PAYMENT] → [OUTBOX] → [OUTBOX-PUBLISHER] → [KAFKA] → [DOCUMENT]` 순서로 찍히고,
`documents` 테이블에 해당 `paymentId`의 행이 하나 생긴다.

### 시나리오 B — Document 생성 실패 → Retry → DLT

```bash
curl -X POST http://localhost:8080/api/payments -H "Content-Type: application/json" \
  -d '{"orderId":"ORDER-FAIL-001"}'
curl -X POST http://localhost:8080/api/payments/{id}/document-failure
curl -X POST http://localhost:8080/api/payments/{id}/complete
```

`[DOCUMENT] 이벤트 수신` 로그가 3번(최초 1회 + 재시도 2회) 찍힌 뒤 `[DLT] 문서 생성 최종 실패`
로그가 찍힌다. `documents` 테이블에는 해당 `paymentId` 행이 생기지 않는다.

### 시나리오 C — 멱등성 확인

같은 `PaymentCompletedEvent`가 두 번 소비되어도(예: 컨슈머 재시작으로 인한 재전달) `documents`
테이블에는 `paymentId`당 행이 하나만 남는다 — `DocumentServiceTest`에서 자동화 테스트로 확인한다.

요청 모음은 `http/payment.http`에 있다.
자동화 테스트는 `src/test/java/com/young/studyproject/payment/application/PaymentServiceTest.java`,
`src/test/java/com/young/studyproject/document/application/DocumentServiceTest.java`,
`src/test/java/com/young/studyproject/outbox/PaymentKafkaFlowTest.java`.
