package com.young.studyproject.example.record.dto;

public record EqualityResponse(
        UserCard first,
        UserCard second,
        boolean equalsResult,
        boolean sameReference,
        int hashSetSize
) {
}
