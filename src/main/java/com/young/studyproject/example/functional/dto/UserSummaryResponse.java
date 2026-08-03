package com.young.studyproject.example.functional.dto;

import com.young.studyproject.user.domain.User;

public record UserSummaryResponse(Long id, String name, String email) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getName(), user.getEmail());
    }
}
