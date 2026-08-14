# 예제: 비동기 처리 (`CompletableFuture` / 가상 스레드 / `@Async`)

느린 작업을 어떻게 동시에 처리하는지를 실제 API로 호출해 확인하는 예제.
응답에 **어느 스레드에서 실행됐는지(`threadName`)** 와 **총 소요 시간(`totalElapsedMs`)** 이 담겨 있어,
순차 실행과 비교하면 차이가 바로 보인다.

## 구성

- `com.young.studyproject.common.config.AsyncConfig`
  - `@EnableAsync` + 예제 전용 스레드 풀 `exampleTaskExecutor` (core 4 / max 8 / queue 100, 접두사 `example-async-`)
- `com.young.studyproject.example.async.AsyncExampleService`
  - `CompletableFuture` 관련 예제 전체
- `com.young.studyproject.example.async.SpringAsyncExampleService`
  - `@Async`와 self-invocation 문제 확인용 별도 빈
- `com.young.studyproject.example.async.dto`
  - 응답용 record 5종 (`TaskResult`, `AsyncExecutionResponse`, `ChainResponse`, `FailureHandlingResponse`, `TimeoutResponse`)

DB는 쓰지 않고 `Thread.sleep`으로 느린 외부 호출을 흉내 낸다.
JPA의 `EntityManager`와 트랜잭션은 스레드에 묶여 있어(ThreadLocal) 다른 스레드로 넘기면 트랜잭션 밖에서 실행되기 때문에,
비동기 자체를 익히는 단계에서는 repository를 끌어들이지 않는다.

## 엔드포인트 ↔ 학습 포인트

| 엔드포인트 | 주제 | 설명 |
|---|---|---|
| `GET /api/examples/async/sequential?count=&delayMs=` | 비교 기준 | 요청 스레드에서 하나씩 실행. 총 시간 ≈ 작업 시간의 합 |
| `GET /api/examples/async/parallel?count=&delayMs=` | `supplyAsync` + `allOf` | 모두 띄운 뒤 한 번에 대기. 총 시간 ≈ 가장 느린 작업 하나 |
| `GET /api/examples/async/virtual-threads?count=&delayMs=` | 가상 스레드 | `Executors.newVirtualThreadPerTaskExecutor()`. 풀 크기 제한 없이 동시 대기 |
| `GET /api/examples/async/spring-async?delayMs=` | `@Async` | 프록시 호출 vs self-invocation의 실행 스레드 비교 |
| `GET /api/examples/async/chain?name=&delayMs=` | 조합 | `thenApply` / `thenCompose` / `thenCombine`의 차이 |
| `GET /api/examples/async/exception?fail=&delayMs=` | 예외 처리 | `exceptionally` / `handle` / `join`의 `CompletionException` |
| `GET /api/examples/async/timeout?delayMs=&timeoutMs=` | 타임아웃 | `orTimeout`(실패) vs `completeOnTimeout`(대체값) |

파라미터 범위는 `count` 1~50 (가상 스레드는 1~1000), `delayMs`/`timeoutMs` 0~3000이며 벗어나면 `IllegalArgumentException`이다.

## 정리해 둘 내용

### 1. `supplyAsync`는 호출 즉시 시작된다

```java
// 병렬: 전부 띄운 뒤에 기다린다
List<CompletableFuture<TaskResult>> futures = ...map(i -> CompletableFuture.supplyAsync(...)).toList();
futures.stream().map(CompletableFuture::join).toList();

// 순차: 하나 띄우고 바로 기다리므로 결국 직렬 실행 (자주 하는 실수)
...map(i -> CompletableFuture.supplyAsync(...).join()).toList();
```

`allOf`는 `CompletableFuture<Void>`라 결과값을 주지 않는다. 완료 대기용으로만 쓰고 값은 각 future에서 꺼낸다.

### 2. executor를 지정하지 않으면 `ForkJoinPool.commonPool()`

commonPool은 `CPU 코어 수 - 1` 크기라서 blocking I/O를 올리면 애플리케이션 전체가 함께 느려진다.
그래서 이 예제는 전용 `exampleTaskExecutor`를 넘긴다.
`parallel?count=20`으로 풀 크기(max 8)를 넘겨 호출하면 뒤쪽 작업이 큐에서 대기해 총 시간이 늘어나는 것을 볼 수 있다.

### 3. 가상 스레드는 풀 크기에 막히지 않는다

`virtual-threads?count=200`도 총 시간이 `delayMs` 근처에 머문다.
blocking 대기가 많은 작업에 적합하고, CPU를 계속 쓰는 작업에는 이점이 없다.

작업 하나가 실패해도 `exceptionally`로 감싸 실패 결과(`value`에 `"실패: ..."`)로 남기므로,
join()에서 CompletionException이 던져져 나머지 작업 결과까지 통째로 버려지는 일은 없다.

### 4. `@Async`는 AOP 프록시로 동작한다

- **다른 빈**이 호출해야 프록시를 거쳐 별도 스레드에서 실행된다.
- 같은 클래스 안에서 `this`로 호출하면 프록시를 거치지 않아 그냥 동기 메서드가 된다(self-invocation).
- `@EnableAsync`가 없으면 `@Async`는 조용히 무시된다.
- 반환 타입은 `void` 또는 `CompletableFuture`. `void`면 결과도 예외도 호출자가 알 수 없다.
- executor 이름을 지정하지 않으면 Spring Boot 기본 executor(`applicationTaskExecutor`)가 쓰인다.

`spring-async` 응답에서 `proxy-call`의 `threadName`은 `example-async-N`, `self-invocation`은 요청 스레드(`http-nio-...`)로 나온다.

### 5. `thenApply` vs `thenCompose`

- `thenApply`: 결과를 일반 값으로 변환 (Stream의 `map`)
- `thenCompose`: 결과로 또 다른 `CompletableFuture`를 반환할 때 중첩을 평탄화 (Stream의 `flatMap`)
- `thenApply`에 future를 반환하는 함수를 넣으면 `CompletableFuture<CompletableFuture<String>>`가 되어 `join()`을 두 번 해야 한다.
- `thenCombine`: 서로 독립적인 두 작업을 먼저 각각 띄워두면 병렬로 진행되고, 둘 다 끝난 뒤 결과를 합친다.

### 6. 예외는 future 안에 담긴다

비동기 작업 안에서 던진 예외는 호출한 스레드의 try-catch로 잡히지 않는다.

- `exceptionally`: 실패했을 때만 호출되어 대체값 생성
- `handle`: 성공/실패 모두 호출되어 `(결과, 예외)`를 함께 받음
- `join`: 실패하면 원래 예외를 `CompletionException`으로 감싸서 던진다. 원인은 `getCause()`로 꺼낸다.

### 7. 타임아웃은 작업을 취소하지 않는다

- `orTimeout`: 제한 시간을 넘기면 `TimeoutException`으로 실패
- `completeOnTimeout`: 제한 시간을 넘기면 지정한 기본값으로 성공

둘 다 결과를 더 기다리지 않을 뿐이고, 원래 작업 스레드는 계속 돌아간다.

### 8. `InterruptedException`을 삼키지 않기

`catch`에서 아무것도 하지 않으면 상위 코드가 중단 요청을 알 수 없다. `Thread.currentThread().interrupt()`로 상태를 복구한다.

## 실행 및 테스트

```bash
./gradlew bootRun
```

```bash
# 순차(기준) vs 병렬 - 총 소요 시간 비교
curl "http://localhost:8080/api/examples/async/sequential?count=3&delayMs=300"
curl "http://localhost:8080/api/examples/async/parallel?count=3&delayMs=300"

# 풀 크기(max 8)를 넘기면 큐에서 대기 vs 가상 스레드는 제한 없음
curl "http://localhost:8080/api/examples/async/parallel?count=20&delayMs=300"
curl "http://localhost:8080/api/examples/async/virtual-threads?count=200&delayMs=300"

# @Async 프록시 호출 vs self-invocation
curl "http://localhost:8080/api/examples/async/spring-async?delayMs=300"

# 조합 / 예외 / 타임아웃
curl "http://localhost:8080/api/examples/async/chain?name=young&delayMs=200"
curl "http://localhost:8080/api/examples/async/exception?fail=true&delayMs=100"
curl "http://localhost:8080/api/examples/async/timeout?delayMs=1000&timeoutMs=300"
```

요청 모음은 `http/async-example.http`에 있다.
자동화 테스트는 `src/test/java/com/young/studyproject/example/async/AsyncExampleServiceTest.java`.
