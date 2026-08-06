package com.young.studyproject.common.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 예제(example.async)에서 사용할 스레드 풀 설정.
 *
 * <p>@EnableAsync가 있어야 @Async가 프록시를 통해 동작한다. 없으면 @Async는 그냥 무시되고
 * 호출 스레드에서 동기 실행된다.
 *
 * <p>기본 executor를 쓰지 않고 별도 빈을 만드는 이유:
 * CompletableFuture.supplyAsync(...)에 executor를 넘기지 않으면 ForkJoinPool.commonPool()을 쓰는데,
 * commonPool은 CPU 코어 수 - 1 크기라 blocking I/O 작업을 올리면 전체 애플리케이션이 함께 느려진다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor exampleTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);        // 항상 살아있는 스레드 수
        executor.setMaxPoolSize(8);         // 큐가 가득 찼을 때까지 늘어날 수 있는 최대 스레드 수
        executor.setQueueCapacity(100);     // corePoolSize를 넘는 요청이 먼저 쌓이는 큐
        executor.setThreadNamePrefix("example-async-");
        executor.initialize();
        return executor;
    }
}
