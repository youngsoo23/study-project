package com.young.studyproject.example.async.dto;

/**
 * 비동기 작업이 실패했을 때 exceptionally / handle / join이 각각 어떻게 반응하는지 비교한다.
 */
public record FailureHandlingResponse(
        boolean taskFailed,
        String exceptionallyResult,
        String handleResult,
        String joinThrownType,
        String joinCauseType,
        String joinCauseMessage,
        String description
) {
}
