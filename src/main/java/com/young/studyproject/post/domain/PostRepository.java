package com.young.studyproject.post.domain;

import java.util.List;
import java.util.Optional;

/**
 * 도메인과 인프라스트럭쳐(JPA/QueryDSL) 사이의 포트.
 */
public interface PostRepository {

    Post save(Post post);

    Optional<Post> findById(Long id);

    List<Post> findAll();

    List<Post> search(String keyword, Long userId);

    void deleteById(Long id);
}
