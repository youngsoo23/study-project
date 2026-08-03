package com.young.studyproject.example.functional.dto;

import com.young.studyproject.post.domain.Post;
import java.time.LocalDateTime;

public record PostSummaryResponse(Long id, String title, Long userId, LocalDateTime updatedAt) {

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(post.getId(), post.getTitle(), post.getUserId(), post.getUpdatedAt());
    }
}
