package com.young.studyproject.user.application.dto;

import com.young.studyproject.user.domain.User;
import java.time.LocalDateTime;

public record UserResult(Long id, String name, String email, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static UserResult from(User user) {
        return new UserResult(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt(), user.getUpdatedAt());
    }
}
