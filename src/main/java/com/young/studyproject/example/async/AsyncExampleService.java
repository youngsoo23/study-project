package com.young.studyproject.example.async;

import com.young.studyproject.example.async.dto.AsyncExecutionResponse;
import com.young.studyproject.example.async.dto.ChainResponse;
import com.young.studyproject.example.async.dto.FailureHandlingResponse;
import com.young.studyproject.example.async.dto.TaskResult;
import com.young.studyproject.example.async.dto.TimeoutResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * CompletableFuture를 이용한 비동기 처리 학습용 예제.
 *
 * <p>DB를 쓰지 않고 Thread.sleep으로 "느린 외부 호출"을 흉내 낸다.
 * JPA의 EntityManager와 트랜잭션은 스레드에 묶여 있어서(ThreadLocal) 다른 스레드로 넘어가면
 * 트랜잭션 밖에서 실행된다. 비동기 자체를 익히는 단계에서는 repository를 끌어들이지 않는 편이 안전하다.
 *
 * <p>이 클래스에는 @Transactional을 붙이지 않는다. 비동기 작업은 호출한 트랜잭션을 물려받지 않기 때문이다.
 */
@Service
@RequiredArgsConstructor
public class AsyncExampleService {

    private static final long MAX_COUNT = 50;
    private static final long MAX_DELAY_MS = 3_000;
    private static final long MAX_VIRTUAL_COUNT = 1_000;

    // 필드명을 빈 이름(AsyncConfig#exampleTaskExecutor)과 같게 두면
    // Executor 타입 빈이 여러 개여도 이름으로 정확히 주입된다.
    private final Executor exampleTaskExecutor;
    private final SpringAsyncExampleService springAsyncExampleService;

    /**
     * 순차 실행: 작업을 하나씩 끝날 때까지 기다린다. 총 시간 ≈ 각 작업 시간의 합.
     */
    public AsyncExecutionResponse runSequentially(int count, long delayMs) {
        validate(count, delayMs, MAX_COUNT);
        long start = System.nanoTime();

        List<TaskResult> tasks = IntStream.rangeClosed(1, count)
                .mapToObj(i -> slowTask("task-" + i, delayMs))
                .toList();

        return AsyncExecutionResponse.of(
                "sequential",
                "요청 스레드에서 하나씩 실행. 총 시간이 작업 시간의 합에 가깝다.",
                start,
                tasks
        );
    }

    /**
     * 병렬 실행: supplyAsync로 모두 띄운 뒤 allOf로 전부 끝나기를 기다린다.
     * 총 시간 ≈ 가장 오래 걸린 작업 하나.
     *
     * <p>주의: supplyAsync를 호출하는 순간 이미 실행이 시작된다. 아래처럼 먼저 리스트로 모아둬야
     * 병렬이 된다. stream().map(...).map(CompletableFuture::join) 처럼 한 줄로 이으면
     * 하나 띄우고 바로 기다리는 꼴이라 결국 순차 실행이 된다.
     */
    public AsyncExecutionResponse runInParallel(int count, long delayMs) {
        validate(count, delayMs, MAX_COUNT);
        long start = System.nanoTime();

        List<CompletableFuture<TaskResult>> futures = IntStream.rangeClosed(1, count)
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> slowTask("task-" + i, delayMs), exampleTaskExecutor))
                .toList();

        // allOf는 CompletableFuture<Void>라 결과를 못 받는다. 완료 대기용으로만 쓰고
        // 값은 각 future에서 join으로 꺼낸다. (이미 완료됐으므로 여기서는 더 기다리지 않는다.)
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        List<TaskResult> tasks = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        return AsyncExecutionResponse.of(
                "parallel",
                "supplyAsync로 모두 띄운 뒤 allOf로 대기. 총 시간이 가장 느린 작업 하나에 가깝다.",
                start,
                tasks
        );
    }

    /**
     * 가상 스레드(Java 21+): 작업마다 스레드를 하나씩 만들어도 부담이 적다.
     * 스레드 풀 크기에 막히지 않으므로 blocking I/O를 대량으로 동시에 기다리기 좋다.
     * 응답의 threadName이 {@code VirtualThread[#..]} 형태이고 virtualThread=true인 것을 확인한다.
     *
     * <p>작업 하나가 실패해도 전체 요청이 실패하지 않도록 future마다 exceptionally로 감싼다.
     * 감싸지 않으면 join()에서 CompletionException이 던져져 아직 끝나지 않은 다른 작업의 결과까지 버려진다.
     */
    public AsyncExecutionResponse runOnVirtualThreads(int count, long delayMs) {
        validate(count, delayMs, MAX_VIRTUAL_COUNT);
        long start = System.nanoTime();

        // try-with-resources: close()가 이미 제출된 작업이 끝날 때까지 기다려준다.
        try (ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<TaskResult>> futures = IntStream.rangeClosed(1, count)
                    .mapToObj(i -> {
                        String taskName = "task-" + i;
                        long taskStart = System.nanoTime();
                        return CompletableFuture
                                .supplyAsync(() -> slowTask(taskName, delayMs), virtualExecutor)
                                .exceptionally(throwable -> failedTaskResult(taskName, taskStart, throwable));
                    })
                    .toList();

            List<TaskResult> tasks = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            return AsyncExecutionResponse.of(
                    "virtual-threads",
                    "작업 수만큼 가상 스레드를 만들어 실행. 개별 작업이 실패해도 exceptionally로 감싸 다른 작업 결과에는 영향을 주지 않는다.",
                    start,
                    tasks
            );
        }
    }

    /**
     * Spring @Async 비교. 두 작업의 threadName을 보면 차이가 바로 보인다.
     *
     * <ul>
     *   <li>proxy-call: 다른 빈(this가 아닌)을 통해 호출 -> 프록시를 거쳐 example-async-N 스레드에서 실행
     *   <li>self-invocation: 같은 클래스 안에서 this로 호출 -> 프록시를 못 거쳐 요청 스레드에서 동기 실행
     * </ul>
     */
    public AsyncExecutionResponse runSpringAsync(long delayMs) {
        validate(1, delayMs, MAX_COUNT);
        long start = System.nanoTime();

        // 먼저 띄워두고(비동기 시작) 아래 동기 작업과 겹치게 만든다.
        CompletableFuture<TaskResult> proxyCall = springAsyncExampleService.runAsync("proxy-call", delayMs);
        TaskResult selfInvocation = springAsyncExampleService.runBySelfInvocation(delayMs);

        return AsyncExecutionResponse.of(
                "spring-async",
                "@Async는 AOP 프록시로 동작한다. 같은 클래스 내부 호출(self-invocation)에는 적용되지 않는다.",
                start,
                List.of(proxyCall.join(), selfInvocation)
        );
    }

    /**
     * thenApply / thenCompose / thenCombine 비교.
     *
     * <ul>
     *   <li>thenApply: 결과를 일반 값으로 변환 (Stream의 map)
     *   <li>thenCompose: 결과로 또 다른 CompletableFuture를 반환할 때 중첩을 평탄화 (Stream의 flatMap)
     *   <li>thenCombine: 서로 독립적인 두 작업이 모두 끝난 뒤 두 결과를 합침
     * </ul>
     */
    public ChainResponse runChain(String name, long delayMs) {
        validate(1, delayMs, MAX_COUNT);

        // thenApply: String -> String
        String thenApplyResult = fetchUserName(name, delayMs)
                .thenApply(String::toUpperCase)
                .join();

        // thenCompose: String -> CompletableFuture<String> 을 이어붙임
        String thenComposeResult = fetchUserName(name, delayMs)
                .thenCompose(userName -> fetchGreeting(userName, delayMs))
                .join();

        // 같은 함수를 thenApply에 넣으면 CompletableFuture<CompletableFuture<String>>로 중첩된다.
        // 값을 꺼내려면 join()을 두 번 해야 한다. 그래서 이 경우엔 thenCompose를 쓴다.
        CompletableFuture<CompletableFuture<String>> nested = fetchUserName(name, delayMs)
                .thenApply(userName -> fetchGreeting(userName, delayMs));
        String nestedTypeWithThenApply =
                "CompletableFuture<CompletableFuture<String>> -> join().join() = " + nested.join().join();

        // thenCombine: 두 작업을 먼저 각각 띄워두면 둘이 병렬로 진행된다.
        CompletableFuture<String> userFuture = fetchUserName(name, delayMs);
        CompletableFuture<String> postFuture = fetchLatestPostTitle(delayMs);
        String thenCombineResult = userFuture
                .thenCombine(postFuture, (userName, postTitle) -> userName + "님의 최신 글: " + postTitle)
                .join();

        return new ChainResponse(
                thenApplyResult,
                thenComposeResult,
                nestedTypeWithThenApply,
                thenCombineResult,
                "thenApply=값 변환(map), thenCompose=Future 이어붙이기(flatMap), thenCombine=독립 작업 두 개 합치기"
        );
    }

    /**
     * 예외 처리 비교.
     *
     * <ul>
     *   <li>비동기 작업 안에서 던진 예외는 호출한 스레드의 try-catch로 잡히지 않는다.
     *   <li>exceptionally: 실패했을 때만 호출되어 대체값을 만든다.
     *   <li>handle: 성공/실패 모두 호출되어 (결과, 예외)를 함께 받는다.
     *   <li>join: 실패하면 원래 예외를 CompletionException으로 감싸서 던진다. 원인은 getCause()로 꺼낸다.
     * </ul>
     */
    public FailureHandlingResponse runWithFailure(boolean fail, long delayMs) {
        validate(1, delayMs, MAX_COUNT);

        String exceptionallyResult = riskyTask(fail, delayMs)
                .exceptionally(throwable -> "exceptionally 대체값 (원인: " + throwable.getCause().getMessage() + ")")
                .join();

        String handleResult = riskyTask(fail, delayMs)
                .handle((result, throwable) -> throwable == null
                        ? "handle 정상 처리: " + result
                        : "handle 실패 처리: " + throwable.getCause().getMessage())
                .join();

        String joinThrownType = "던져지지 않음";
        String joinCauseType = "없음";
        String joinCauseMessage = "없음";
        try {
            riskyTask(fail, delayMs).join();
        } catch (CompletionException e) {
            joinThrownType = e.getClass().getSimpleName();
            joinCauseType = e.getCause().getClass().getSimpleName();
            joinCauseMessage = e.getCause().getMessage();
        }

        return new FailureHandlingResponse(
                fail,
                exceptionallyResult,
                handleResult,
                joinThrownType,
                joinCauseType,
                joinCauseMessage,
                "exceptionally=실패할 때만, handle=성공/실패 모두, join=CompletionException으로 감싸서 다시 던짐"
        );
    }

    /**
     * 타임아웃 비교.
     *
     * <ul>
     *   <li>orTimeout: 제한 시간을 넘기면 TimeoutException으로 실패시킨다.
     *   <li>completeOnTimeout: 제한 시간을 넘기면 지정한 기본값으로 성공 처리한다.
     * </ul>
     *
     * <p>둘 다 원래 작업을 중단시키지는 않는다. 결과를 더 기다리지 않을 뿐 작업은 계속 돌아간다.
     */
    public TimeoutResponse runWithTimeout(long delayMs, long timeoutMs) {
        validate(1, delayMs, MAX_COUNT);
        if (timeoutMs <= 0 || timeoutMs > MAX_DELAY_MS) {
            throw new IllegalArgumentException("timeoutMs는 1 이상 " + MAX_DELAY_MS + " 이하여야 합니다.");
        }

        String orTimeoutResult;
        try {
            orTimeoutResult = CompletableFuture
                    .supplyAsync(() -> slowTask("or-timeout", delayMs).value(), exampleTaskExecutor)
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException e) {
            orTimeoutResult = e.getCause() instanceof TimeoutException
                    ? "TimeoutException 발생 (제한 " + timeoutMs + "ms 초과)"
                    : "실패: " + e.getCause().getMessage();
        }

        String completeOnTimeoutResult = CompletableFuture
                .supplyAsync(() -> slowTask("complete-on-timeout", delayMs).value(), exampleTaskExecutor)
                .completeOnTimeout("기본값 (제한 " + timeoutMs + "ms 초과)", timeoutMs, TimeUnit.MILLISECONDS)
                .join();

        return new TimeoutResponse(
                delayMs,
                timeoutMs,
                orTimeoutResult,
                completeOnTimeoutResult,
                "orTimeout=TimeoutException으로 실패, completeOnTimeout=대체값으로 성공. 둘 다 원래 작업을 취소하지는 않는다."
        );
    }

    // --- 내부 헬퍼 ---

    private CompletableFuture<String> fetchUserName(String name, long delayMs) {
        return CompletableFuture.supplyAsync(
                () -> slowTask("fetchUserName", delayMs).value() + ":" + name,
                exampleTaskExecutor);
    }

    private CompletableFuture<String> fetchGreeting(String userName, long delayMs) {
        return CompletableFuture.supplyAsync(
                () -> {
                    slowTask("fetchGreeting", delayMs);
                    return "안녕하세요, " + userName + "님";
                },
                exampleTaskExecutor);
    }

    private CompletableFuture<String> fetchLatestPostTitle(long delayMs) {
        return CompletableFuture.supplyAsync(
                () -> {
                    slowTask("fetchLatestPostTitle", delayMs);
                    return "비동기 처리 정리";
                },
                exampleTaskExecutor);
    }

    private CompletableFuture<String> riskyTask(boolean fail, long delayMs) {
        return CompletableFuture.supplyAsync(
                () -> {
                    slowTask("riskyTask", delayMs);
                    if (fail) {
                        // 이 예외는 호출한 스레드로 바로 전파되지 않고 future 안에 담긴다.
                        throw new IllegalStateException("비동기 작업이 의도적으로 실패했습니다.");
                    }
                    return "정상 결과";
                },
                exampleTaskExecutor);
    }

    /**
     * 느린 외부 호출(HTTP, 파일, 외부 API 등)을 흉내 내는 작업.
     */
    private TaskResult slowTask(String taskName, long delayMs) {
        long start = System.nanoTime();
        sleep(delayMs);
        return TaskResult.of(taskName, start, taskName + " 완료");
    }

    /**
     * exceptionally에서 받은 예외를 실패 결과를 나타내는 TaskResult로 변환한다.
     * throwable은 join()과 마찬가지로 CompletionException으로 감싸져 있으므로 getCause()로 원인을 꺼낸다.
     */
    TaskResult failedTaskResult(String taskName, long startNanos, Throwable throwable) {
        Thread current = Thread.currentThread();
        return new TaskResult(
                taskName,
                current.getName(),
                current.isVirtual(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                "실패: " + throwable.getCause().getMessage()
        );
    }

    /**
     * InterruptedException을 삼키지 않고 인터럽트 상태를 복구한다.
     * catch에서 아무것도 하지 않으면 상위 코드가 중단 요청을 알 수 없다.
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("작업이 중단되었습니다.", e);
        }
    }

    private void validate(int count, long delayMs, long maxCount) {
        if (count < 1 || count > maxCount) {
            throw new IllegalArgumentException("count는 1 이상 " + maxCount + " 이하여야 합니다.");
        }
        if (delayMs < 0 || delayMs > MAX_DELAY_MS) {
            throw new IllegalArgumentException("delayMs는 0 이상 " + MAX_DELAY_MS + " 이하여야 합니다.");
        }
    }
}
