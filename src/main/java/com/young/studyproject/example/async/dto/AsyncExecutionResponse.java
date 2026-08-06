package com.young.studyproject.example.async.dto;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 여러 작업을 실행한 전체 결과.
 *
 * <p>totalElapsedMs를 각 작업의 elapsedMs 합과 비교하면 순차 실행과 병렬 실행의 차이가 드러난다.
 * 순차면 총 시간 ≈ 작업 시간의 합, 병렬이면 총 시간 ≈ 가장 오래 걸린 작업 하나.
 */
public record AsyncExecutionResponse(
        String mode,
        String description,
        String callerThread,
        long totalElapsedMs,
        List<TaskResult> tasks
) {

    public static AsyncExecutionResponse of(
            String mode,
            String description,
            long startNanos,
            List<TaskResult> tasks
    ) {
        return new AsyncExecutionResponse(
                mode,
                description,
                Thread.currentThread().getName(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos),
                tasks
        );
    }
}
