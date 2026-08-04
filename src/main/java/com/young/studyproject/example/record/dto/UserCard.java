package com.young.studyproject.example.record.dto;

import com.young.studyproject.user.domain.User;

/**
 * Lombok @Data와 비교되는 지점: @Data는 setter까지 만들어주는 mutable 객체지만,
 * record는 필드가 전부 private final인 불변 객체다.
 */
public record UserCard(Long id, String name, String email) {

    public static UserCard from(User user) {
        return new UserCard(user.getId(), user.getName(), user.getEmail());
    }
}
