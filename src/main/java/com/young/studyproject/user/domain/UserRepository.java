package com.young.studyproject.user.domain;

import java.util.List;
import java.util.Optional;

/**
 * 도메인과 인프라스트럭쳐(JPA/QueryDSL) 사이의 포트.
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    List<User> findAll();

    List<User> search(String name);

    boolean existsByEmail(String email);

    void deleteById(Long id);
}
