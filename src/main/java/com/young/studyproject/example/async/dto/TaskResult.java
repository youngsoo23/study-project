package com.young.studyproject.example.async.dto;

import java.util.concurrent.TimeUnit;

/**
 * 개별 비동기 작업 하나의 실행 결과.
 *
 * <p>어떤 스레드에서 실행됐는지(threadName, virtualThread)를 응답에 그대로 담아
 * "정말 다른 스레드에서 돌았는지"를 눈으로 확인하는 것이 이 예제의 핵심이다.
 */
public record TaskResult(
        String taskName,
        String threadName,
        boolean virtualThread,
        long elapsedMs,
        String value
) {

    public static TaskResult of(String taskName, long startNanos, String value) {
        Thread current = Thread.currentThread();
        return new TaskResult(
                taskName,
                current.getName(),
                current.isVirtual(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                value
        );
    }
}
