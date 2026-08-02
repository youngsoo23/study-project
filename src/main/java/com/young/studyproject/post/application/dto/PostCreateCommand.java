package com.young.studyproject.post.application.dto;

public record PostCreateCommand(String title, String content, Long userId) {
}
