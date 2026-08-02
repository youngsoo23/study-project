package com.young.studyproject.user.domain;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class User {

    private final Long id;
    private final String name;
    private final String email;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public User update(String name, String email) {
        return User.builder()
                .id(this.id)
                .name(name)
                .email(email)
                .createdAt(this.createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
