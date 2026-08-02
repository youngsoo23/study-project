package com.young.studyproject.user.presentation.dto;

import com.young.studyproject.user.application.dto.UserResult;
import java.time.LocalDateTime;

public record UserResponse(Long id, String name, String email, LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static UserResponse from(UserResult result) {
        return new UserResponse(result.id(), result.name(), result.email(), result.createdAt(), result.updatedAt());
    }
}
