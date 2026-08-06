package com.young.studyproject.example.async.dto;

/**
 * orTimeout(예외로 실패) 과 completeOnTimeout(대체값으로 성공) 의 차이를 비교한다.
 */
public record TimeoutResponse(
        long delayMs,
        long timeoutMs,
        String orTimeoutResult,
        String completeOnTimeoutResult,
        String description
) {
}
