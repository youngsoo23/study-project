package com.young.studyproject.example.async.dto;

/**
 * thenApply / thenCompose / thenCombine의 차이를 한 번에 비교하기 위한 응답.
 */
public record ChainResponse(
        String thenApplyResult,
        String thenComposeResult,
        String nestedTypeWithThenApply,
        String thenCombineResult,
        String description
) {
}
