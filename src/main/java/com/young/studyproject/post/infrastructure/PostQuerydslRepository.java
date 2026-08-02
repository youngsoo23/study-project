package com.young.studyproject.post.infrastructure;

import java.util.List;

public interface PostQuerydslRepository {

    List<PostJpaEntity> search(String keyword, Long userId);
}
