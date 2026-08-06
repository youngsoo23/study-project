package com.young.studyproject.example.async;

import com.young.studyproject.example.async.dto.TaskResult;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Spring의 @Async 학습용 예제.
 *
 * <p>@Async는 AOP 프록시로 동작한다. 즉 <b>다른 빈이 프록시를 통해 호출</b>해야 별도 스레드에서 실행된다.
 * 같은 클래스 안에서 this로 호출하면 프록시를 거치지 않아 그냥 동기 메서드가 된다(self-invocation 문제).
 *
 * <p>반환 타입은 void 또는 CompletableFuture를 쓴다. void면 결과도 예외도 호출자가 알 수 없다.
 */
@Service
public class SpringAsyncExampleService {

    /**
     * 프록시를 통해 호출되면 exampleTaskExecutor의 스레드에서 실행된다.
     * executor 이름을 지정하지 않으면 Spring Boot 기본 executor(applicationTaskExecutor)가 쓰인다.
     */
    @Async("exampleTaskExecutor")
    public CompletableFuture<TaskResult> runAsync(String taskName, long delayMs) {
        long start = System.nanoTime();
        sleep(delayMs);
        return CompletableFuture.completedFuture(TaskResult.of(taskName, start, taskName + " 완료"));
    }

    /**
     * 같은 클래스 안에서 @Async 메서드를 호출한다.
     * 응답의 threadName이 요청 스레드(http-nio-...)와 같게 나오는 것을 확인한다. 비동기가 아니다.
     */
    public TaskResult runBySelfInvocation(long delayMs) {
        return runAsync("self-invocation", delayMs).join();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("작업이 중단되었습니다.", e);
        }
    }
}
