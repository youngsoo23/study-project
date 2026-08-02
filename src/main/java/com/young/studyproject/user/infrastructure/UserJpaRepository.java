package com.young.studyproject.user.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long>, UserQuerydslRepository {

    boolean existsByEmail(String email);
}
