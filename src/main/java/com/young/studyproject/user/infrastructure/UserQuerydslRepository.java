package com.young.studyproject.user.infrastructure;

import java.util.List;

public interface UserQuerydslRepository {

    List<UserJpaEntity> search(String name);
}
