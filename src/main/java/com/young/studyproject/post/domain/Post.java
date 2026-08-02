package com.young.studyproject.post.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Post {

    private final Long id;
    private final String title;
    private final String content;
    private final Long userId;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public Post update(String title, String content) {
        return Post.builder()
                .id(this.id)
                .title(title)
                .content(content)
                .userId(this.userId)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
