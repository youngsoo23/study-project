package com.young.studyproject.user.infrastructure;

import static com.young.studyproject.user.infrastructure.QUserJpaEntity.userJpaEntity;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserQuerydslRepositoryImpl implements UserQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UserJpaEntity> search(String name) {
        return queryFactory
                .selectFrom(userJpaEntity)
                .where(nameContains(name))
                .fetch();
    }

    private BooleanExpression nameContains(String name) {
        return (name == null || name.isBlank()) ? null : userJpaEntity.name.containsIgnoreCase(name);
    }
}
