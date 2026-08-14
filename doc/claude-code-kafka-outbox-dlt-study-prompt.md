# Claude Code Prompt — Spring Kafka Outbox / Retry / DLT 학습 예제

## 목적

현재 스터디 프로젝트에 **비동기 이벤트 처리 흐름을 직접 이해할 수 있는 작은 예제**를 추가해줘.

이번 예제의 핵심 학습 목표는 아래 흐름을 코드로 직접 확인하는 것이다.

```text
결제 완료
  ↓
Payment = COMPLETE 저장
  +
Outbox = PENDING 저장
  ↓
Outbox Publisher
  ↓
Kafka: payment-completed
  ↓
Document Consumer
  ↓
문서 생성
  ↓ 실패
Retry
  ↓ 계속 실패
DLT: payment-completed.DLT
```

중요한 점은 기능을 복잡하게 만드는 것이 아니라,  
**Outbox / Kafka / Retry / DLT가 각각 왜 필요한지 코드 흐름으로 이해할 수 있게 만드는 것**이다.

---

## 작업 전 먼저 확인할 것

현재 프로젝트 구조와 사용 중인 기술 스택을 먼저 확인해줘.

특히 아래를 확인한 뒤 기존 프로젝트 스타일에 맞춰 구현해줘.

- Java / Spring Boot 버전
- Gradle 의존성
- 패키지 구조
- JPA 사용 여부
- DB 종류
- 기존 Entity / Repository / Service 작성 방식
- Docker Compose 사용 여부
- 기존 Kafka 설정 존재 여부

기존 코드 구조를 최대한 유지하고, 학습 예제 때문에 프로젝트 전체 구조를 크게 변경하지 마.

---

# 1. Payment 예제 만들기

간단한 Payment 도메인을 만든다.

예시:

```java
Payment
- id
- orderId
- status
```

상태:

```java
READY
COMPLETE
```

결제 완료 메서드를 만든다.

```java
payment.complete();
```

외부 실제 결제 API는 필요 없다.

학습 목적이므로 API 호출로 결제를 COMPLETE 상태로 변경할 수 있으면 된다.

예:

```http
POST /payments/{paymentId}/complete
```

---

# 2. Outbox 테이블 만들기

결제 완료 시 Kafka에 바로 메시지를 보내지 않는다.

대신 Payment 상태 변경과 Outbox 이벤트 저장을 **하나의 DB Transaction**에서 처리한다.

Outbox Entity 예시:

```text
OutboxEvent

id
eventId
eventType
aggregateId
payload
status
createdAt
publishedAt
```

상태:

```java
PENDING
PUBLISHED
```

eventType:

```text
PAYMENT_COMPLETED
```

예를 들면 결제 완료 시:

```text
payment

id | status
1  | COMPLETE
```

와 동시에:

```text
outbox_event

id | event_type         | aggregate_id | status
1  | PAYMENT_COMPLETED  | 1            | PENDING
```

가 저장되어야 한다.

중요:

```java
@Transactional
public void completePayment(...) {
    // payment COMPLETE
    // outbox PENDING 저장
}
```

두 작업이 반드시 같은 트랜잭션 안에서 실행되도록 구현해줘.

---

# 3. Outbox Publisher 만들기

Outbox에 저장된 `PENDING` 이벤트를 Kafka로 발행하는 Publisher를 만든다.

학습하기 쉽게 우선 `@Scheduled` 기반 polling 방식으로 구현한다.

예:

```java
@Scheduled(fixedDelay = 1000)
```

흐름:

```text
Outbox PENDING 조회
  ↓
Kafka payment-completed Topic 발행
  ↓
성공
  ↓
Outbox PUBLISHED 변경
```

한 번에 너무 많은 메시지를 조회하지 않도록 간단한 제한을 둬도 좋다.

Kafka 발행이 실패하면 Outbox 상태는 `PENDING`으로 남아 다음 스케줄에서 다시 시도할 수 있게 한다.

---

# 4. Kafka Topic

Topic 이름:

```text
payment-completed
```

메시지는 최소한 아래 정보를 포함한다.

```json
{
  "eventId": "uuid",
  "paymentId": 1,
  "orderId": "ORDER-001"
}
```

이 이벤트 클래스도 별도로 만들어줘.

예:

```java
PaymentCompletedEvent
```

---

# 5. Document Consumer 만들기

`payment-completed` 토픽을 구독하는 Consumer를 만든다.

예:

```java
@KafkaListener(
    topics = "payment-completed",
    groupId = "document-service"
)
```

역할은 아주 단순하게 한다.

```text
PAYMENT_COMPLETED 수신
  ↓
문서 생성
```

실제 PDF 생성은 필요 없다.

DB에 Document 데이터를 하나 생성하는 정도로 구현해줘.

예:

```text
Document

id
paymentId
documentNumber
createdAt
```

---

# 6. 일부러 실패시키는 API 또는 조건 만들기

Retry와 DLT를 직접 확인하고 싶다.

특정 paymentId 또는 설정값에 따라 Document Consumer가 일부러 예외를 발생시키도록 만들어줘.

예:

```java
if (paymentId == 특정 값) {
    throw new RuntimeException("문서 생성 강제 실패");
}
```

단, 코드에 숫자를 마구 하드코딩하기보다 학습하기 쉬운 형태로 만들어줘.

예를 들어:

```text
POST /payments/{id}/document-failure
```

처럼 실패 플래그를 설정하거나,

application.yml 설정값을 이용하는 방식도 괜찮다.

가장 단순하고 이해하기 쉬운 방식을 선택해줘.

---

# 7. Kafka Retry 설정

Document Consumer에서 예외가 발생하면 바로 DLT로 보내지 말고 몇 번 재시도하게 한다.

학습하기 쉽게:

```text
1초 간격
총 3회 정도 재시도
```

정도로 설정해줘.

Spring Kafka의 현재 프로젝트 버전에 맞는 권장 방식을 사용한다.

예:

```java
DefaultErrorHandler
DeadLetterPublishingRecoverer
FixedBackOff
```

단, 현재 Spring Kafka 버전에 따라 더 적절한 방식이 있다면 그 방식을 사용해도 된다.

---

# 8. DLT 구성

재시도를 모두 실패한 메시지는 아래 Topic으로 보내도록 한다.

```text
payment-completed.DLT
```

DLT에 들어간 메시지도 원래 이벤트의:

```text
eventId
paymentId
orderId
```

를 확인할 수 있어야 한다.

예를 들어:

```text
payment-completed.DLT

offset 0
→ paymentId = 1 실패 메시지

offset 1
→ paymentId = 2 실패 메시지
```

처럼 서로 다른 실패 이벤트들이 같은 DLT Topic에 각각 저장되는 것을 확인할 수 있어야 한다.

---

# 9. DLT Consumer도 만들어줘

학습을 위해 DLT를 읽는 Consumer도 하나 만든다.

단, DLT 메시지를 자동으로 다시 원본 Topic에 무한 재발행하지 않는다.

우선은 로그만 남긴다.

예:

```java
@KafkaListener(
    topics = "payment-completed.DLT",
    groupId = "document-dlt-monitor"
)
```

로그 예시:

```text
[DLT] 문서 생성 최종 실패
eventId=...
paymentId=...
orderId=...
```

이렇게 해서 내가 실제로 DLT에 메시지가 들어갔다는 것을 콘솔에서 확인할 수 있게 해줘.

---

# 10. 멱등성도 아주 간단히 보여줘

Kafka 메시지는 중복 전달될 수 있다는 점을 학습하고 싶다.

Document 생성 전에:

```java
documentRepository.existsByPaymentId(paymentId)
```

등으로 이미 문서가 생성되었는지 확인해서 중복 생성을 막아줘.

가능하면 DB의 UNIQUE 제약조건도 같이 둬.

예:

```text
Document.paymentId UNIQUE
```

그리고 코드 주석으로:

```text
Kafka는 동일 메시지가 다시 전달될 수 있으므로
Consumer는 멱등성을 고려해야 한다.
```

라고 설명해줘.

---

# 11. 패키지 구조

기존 프로젝트 구조를 먼저 확인하고 그 구조를 우선한다.

새로운 예제가 기존 도메인과 섞이기 어렵다면 아래와 비슷하게 구성해도 된다.

```text
payment
 ├─ domain
 ├─ application
 ├─ infrastructure
 └─ presentation

outbox
 ├─ domain
 ├─ application
 └─ infrastructure

document
 ├─ domain
 ├─ application
 └─ infrastructure
```

하지만 학습 프로젝트이므로 과도한 DDD 계층화는 하지 마.

코드가 어디에서 무엇을 하는지 이해하기 쉬운 것이 최우선이다.

---

# 12. Kafka 실행 환경

현재 프로젝트에 Kafka 실행 환경이 없다면 Docker Compose로 로컬 Kafka를 실행할 수 있게 만들어줘.

가능하면 설정을 최소화해줘.

목표:

```bash
docker compose up -d
```

후 Spring Boot 애플리케이션을 실행하면 테스트할 수 있어야 한다.

Kafka UI가 꼭 필요하지는 않다.

다만 아주 쉽게 추가할 수 있다면 선택적으로 추가해도 된다.

---

# 13. application 설정

필요한 Kafka 설정을 기존 `application.yml` 또는 profile 설정에 추가해줘.

예:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
```

Producer / Consumer JSON Serializer 설정도 프로젝트 버전에 맞게 정상 동작하도록 구성해줘.

---

# 14. 테스트 시나리오를 README에 작성

내가 직접 따라 해볼 수 있도록 README 또는 별도 markdown 문서를 만들어줘.

제목 예시:

```text
Kafka Outbox / Retry / DLT 학습 예제
```

아래 시나리오를 반드시 포함해줘.

## 시나리오 A — 정상 처리

```text
1. Payment 생성
2. 결제 완료 API 호출
3. Payment COMPLETE 확인
4. Outbox PENDING 생성 확인
5. Publisher가 Kafka 발행
6. Outbox PUBLISHED 확인
7. Document Consumer 동작
8. Document 생성 확인
```

## 시나리오 B — Document 생성 실패

```text
1. 실패하도록 Payment 설정
2. 결제 완료
3. Outbox → Kafka 발행
4. Document Consumer 실패
5. Retry 실행
6. 재시도 모두 실패
7. payment-completed.DLT 이동
8. DLT Consumer 로그 확인
```

## 시나리오 C — 멱등성 확인

동일 `PaymentCompletedEvent`가 두 번 들어오더라도 Document가 2개 생성되지 않는 것을 확인한다.

---

# 15. 로그를 적극적으로 넣어줘

학습 목적이므로 중요한 흐름마다 로그를 남겨줘.

예:

```text
[PAYMENT] 결제 완료 paymentId=1

[OUTBOX] 이벤트 저장 eventId=xxx

[OUTBOX-PUBLISHER] Kafka 발행 시작 eventId=xxx

[KAFKA] PAYMENT_COMPLETED 발행 성공 eventId=xxx

[DOCUMENT] 이벤트 수신 paymentId=1

[DOCUMENT] 문서 생성 성공 paymentId=1

[DOCUMENT] 문서 생성 실패 paymentId=2 retry...

[DLT] 최종 실패 메시지 수신 paymentId=2
```

내가 애플리케이션 로그만 봐도 전체 흐름을 따라갈 수 있게 해줘.

---

# 16. 코드에 설명 주석 추가

특히 아래 부분에는 간단한 한글 주석을 추가해줘.

### Outbox 저장 부분

```text
Payment DB 변경과 Kafka 발행은 하나의 트랜잭션으로 묶기 어렵기 때문에
Kafka에 보낼 이벤트를 같은 DB에 먼저 저장한다.
```

### Outbox Publisher

```text
DB에 안전하게 저장된 PENDING 이벤트를 Kafka로 전달한다.
```

### Retry

```text
일시적인 장애일 가능성이 있기 때문에 즉시 실패 처리하지 않고 재시도한다.
```

### DLT

```text
정해진 재시도 횟수를 모두 실패한 메시지를 버리지 않고 별도 Topic에 보관한다.
```

### 멱등성

```text
같은 Kafka 메시지가 재전달될 수 있으므로 중복 처리를 방지한다.
```

---

# 17. 너무 복잡하게 만들지 말 것

이번 구현에서 제외:

- Debezium
- CDC
- Kafka Streams
- Saga Pattern
- 분산 트랜잭션
- Exactly Once 고급 설정
- Kubernetes
- Schema Registry
- Avro
- 복잡한 공통 모듈화

현재 목적은 아래 다섯 개를 확실하게 이해하는 것이다.

```text
1. DB Transaction
2. Outbox
3. Kafka Topic
4. Retry
5. DLT
```

---

# 18. 구현 완료 후 설명

코드를 모두 작성한 뒤 나에게 아래 순서로 설명해줘.

## A. 전체 파일 목록

이번에 생성 / 수정한 파일을 보여줘.

## B. 전체 흐름

아래 흐름을 실제 클래스 이름과 함께 설명해줘.

```text
PaymentService
  ↓
OutboxRepository
  ↓
OutboxPublisher
  ↓
Kafka
  ↓
DocumentConsumer
  ↓
Retry
  ↓
DLT
```

## C. Outbox가 필요한 이유

다음 상황을 기준으로 설명:

```text
Payment DB 저장 성공
Kafka 발행 실패
```

왜 문제가 되는지, Outbox가 어떻게 해결하는지 설명해줘.

## D. DLT가 필요한 이유

다음 상황을 기준으로 설명:

```text
Kafka 메시지 수신 성공
Document 생성 계속 실패
```

왜 DLT로 보내는지 설명해줘.

## E. 직접 테스트 명령

curl 또는 HTTP 요청 예제를 만들어줘.

정상 처리 / 실패 처리 둘 다 직접 실행할 수 있게 해줘.

---

# 최종 목표

코드를 보고 아래 문장을 내가 자연스럽게 이해할 수 있어야 한다.

```text
Outbox는 Kafka로 보내기 전 이벤트 유실을 막기 위한 DB 기반 안전장치다.

Kafka Topic은 발행된 이벤트를 저장하고 Consumer에게 전달한다.

Consumer가 일시적으로 실패하면 Retry한다.

Retry를 모두 실패하면 메시지를 버리지 않고 DLT에 보관한다.

Consumer는 같은 메시지가 다시 올 수 있기 때문에 멱등성을 고려해야 한다.
```

구현 전에 현재 프로젝트를 먼저 분석하고,
기존 코드와 충돌하지 않는 최소한의 변경으로 진행해줘.
