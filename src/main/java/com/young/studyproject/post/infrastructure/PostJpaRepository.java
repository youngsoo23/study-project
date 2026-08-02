package com.young.studyproject.post.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostJpaRepository extends JpaRepository<PostJpaEntity, Long>, PostQuerydslRepository {
}
