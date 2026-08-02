package com.young.studyproject.post.presentation.dto;

import com.young.studyproject.post.application.dto.PostResult;
import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String title,
        String content,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PostResponse from(PostResult result) {
        return new PostResponse(
                result.id(),
                result.title(),
                result.content(),
                result.userId(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
