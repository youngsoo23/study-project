package com.young.studyproject.post.application.dto;

import com.young.studyproject.post.domain.Post;
import java.time.LocalDateTime;

public record PostResult(
        Long id,
        String title,
        String content,
        Long userId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PostResult from(Post post) {
        return new PostResult(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getUserId(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
