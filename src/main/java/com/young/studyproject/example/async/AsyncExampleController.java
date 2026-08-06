package com.young.studyproject.example.async;

import com.young.studyproject.example.async.dto.AsyncExecutionResponse;
import com.young.studyproject.example.async.dto.ChainResponse;
import com.young.studyproject.example.async.dto.FailureHandlingResponse;
import com.young.studyproject.example.async.dto.TimeoutResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 각 엔드포인트는 비동기 처리 방식 하나씩을 직접 확인해본다.
 * 응답의 threadName과 totalElapsedMs를 비교하는 것이 관찰 포인트다.
 */
@RestController
@RequestMapping("/api/examples/async")
@RequiredArgsConstructor
public class AsyncExampleController {

    private final AsyncExampleService exampleService;

    @GetMapping("/sequential") // 비교 기준: 순차 실행
    public AsyncExecutionResponse sequential(
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(defaultValue = "300") long delayMs
    ) {
        return exampleService.runSequentially(count, delayMs);
    }

    @GetMapping("/parallel") // CompletableFuture.supplyAsync + allOf
    public AsyncExecutionResponse parallel(
            @RequestParam(defaultValue = "3") int count,
            @RequestParam(defaultValue = "300") long delayMs
    ) {
        return exampleService.runInParallel(count, delayMs);
    }

    @GetMapping("/virtual-threads") // Executors.newVirtualThreadPerTaskExecutor()
    public AsyncExecutionResponse virtualThreads(
            @RequestParam(defaultValue = "100") int count,
            @RequestParam(defaultValue = "300") long delayMs
    ) {
        return exampleService.runOnVirtualThreads(count, delayMs);
    }

    @GetMapping("/spring-async") // @Async와 self-invocation 문제
    public AsyncExecutionResponse springAsync(@RequestParam(defaultValue = "300") long delayMs) {
        return exampleService.runSpringAsync(delayMs);
    }

    @GetMapping("/chain") // thenApply / thenCompose / thenCombine
    public ChainResponse chain(
            @RequestParam(defaultValue = "young") String name,
            @RequestParam(defaultValue = "200") long delayMs
    ) {
        return exampleService.runChain(name, delayMs);
    }

    @GetMapping("/exception") // exceptionally / handle / join
    public FailureHandlingResponse exception(
            @RequestParam(defaultValue = "true") boolean fail,
            @RequestParam(defaultValue = "100") long delayMs
    ) {
        return exampleService.runWithFailure(fail, delayMs);
    }

    @GetMapping("/timeout") // orTimeout / completeOnTimeout
    public TimeoutResponse timeout(
            @RequestParam(defaultValue = "1000") long delayMs,
            @RequestParam(defaultValue = "300") long timeoutMs
    ) {
        return exampleService.runWithTimeout(delayMs, timeoutMs);
    }
}
